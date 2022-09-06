/*
 * ipneigh.c		"ip neigh".
 *
 *		This program is free software; you can redistribute it and/or
 *		modify it under the terms of the GNU General Public License
 *		as published by the Free Software Foundation; either version
 *		2 of the License, or (at your option) any later version.
 *
 * Authors:	Alexey Kuznetsov, <kuznet@ms2.inr.ac.ru>
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <syslog.h>
#include <fcntl.h>
#include <string.h>
#include <errno.h>
#include <sys/time.h>
#include <sys/socket.h>
#include <net/if.h>
#include <netinet/in.h>
#include <netinet/ip.h>

#include <jni.h>

#include "libnetlink.h"
#include "list.h"
#include "ll_map.h"
#include "utils.h"

// just to remove warning
int print_neigh(const struct sockaddr_nl *who, struct nlmsghdr *n, void *arg);

// from ll_addr.c
const char *ll_addr_n2a(const unsigned char *addr, int alen, int type, char *buf, int blen)
{
	int i;
	int l;

	if (alen == 4 &&
	    (type == ARPHRD_TUNNEL || type == ARPHRD_SIT || type == ARPHRD_IPGRE)) {
		return inet_ntop(AF_INET, addr, buf, blen);
	}
	if (alen == 16 && type == ARPHRD_TUNNEL6) {
		return inet_ntop(AF_INET6, addr, buf, blen);
	}
	snprintf(buf, blen, "%02x", addr[0]);
	for (i = 1, l = 2; i < alen && l < blen; i++, l += 3)
		snprintf(buf + l, blen - l, ":%02x", addr[i]);
	return buf;
}

// from utils.c
int get_user_hz(void)
{
	return sysconf(_SC_CLK_TCK);
}
const char *rt_addr_n2a_r(int af, int len,
			  const void *addr, char *buf, int buflen)
{
	switch (af) {
	case AF_INET:
	case AF_INET6:
		return inet_ntop(af, addr, buf, buflen);
#ifndef ANDROID
	case AF_MPLS:
		return mpls_ntop(af, addr, buf, buflen);
	case AF_IPX:
		return ipx_ntop(af, addr, buf, buflen);
	case AF_DECnet:
	{
		struct dn_naddr dna = { 2, { 0, 0, } };

		memcpy(dna.a_addr, addr, 2);
		return dnet_ntop(af, &dna, buf, buflen);
	}
#endif
	case AF_PACKET:
		return ll_addr_n2a(addr, len, ARPHRD_VOID, buf, buflen);
	default:
		return "???";
	}
}

const char *format_host_r(int af, int len, const void *addr,
			char *buf, int buflen)
{
#ifdef RESOLVE_HOSTNAMES
	if (resolve_hosts) {
		const char *n;

		len = len <= 0 ? af_byte_len(af) : len;

		if (len > 0 &&
		    (n = resolve_address(addr, len, af)) != NULL)
			return n;
	}
#endif
	return rt_addr_n2a_r(af, len, addr, buf, buflen);
}

const char *format_host(int af, int len, const void *addr)
{
	static char buf[256];

	return format_host_r(af, len, addr, buf, 256);
}

int inet_addr_match(const inet_prefix *a, const inet_prefix *b, int bits)
{
	const __u32 *a1 = a->data;
	const __u32 *a2 = b->data;
	int words = bits >> 0x05;

	bits &= 0x1f;

	if (words)
		if (memcmp(a1, a2, words << 2) != 0)
			return -1;

	if (bits) {
		__u32 w1, w2;
		__u32 mask;

		w1 = a1[words];
		w2 = a2[words];

		mask = htonl((0xffffffff) << (0x20 - bits));

		if ((w1 ^ w2) & mask)
			return 1;
	}

	return 0;
}

// from ip.c
struct rtnl_handle rth = { .fd = -1 };

// default ipneight.c
static struct
{
	int family;
	int index;
	int state;
	int unused_only;
	inet_prefix pfx;
	int flushed;
	char *flushb;
	int flushp;
	int flushe;
	int master;
} filter;

static int flush_update(void)
{

	/*
	 * Note that the kernel may delete multiple addresses for one
	 * delete request (e.g. if ipv4 address promotion is disabled).
	 * Since a flush operation is really a series of delete requests
	 * its possible that we may request an address delete that has
	 * already been done by the kernel. Therefore, ignore EADDRNOTAVAIL
	 * errors returned from a flush request
	 */
	if ((rtnl_send_check(&rth, filter.flushb, filter.flushp) < 0) &&
	    (errno != EADDRNOTAVAIL)) {
		perror("Failed to send flush request");
		return -1;
	}
	filter.flushp = 0;
	return 0;
}

int print_neigh(const struct sockaddr_nl *who, struct nlmsghdr *n, void *arg)
{
	FILE *fp = (FILE *)arg;
	struct ndmsg *r = NLMSG_DATA(n);
	int len = n->nlmsg_len;
	struct rtattr *tb[NDA_MAX+1];
	static int show_stats = 0;

	if (n->nlmsg_type != RTM_NEWNEIGH && n->nlmsg_type != RTM_DELNEIGH &&
	    n->nlmsg_type != RTM_GETNEIGH) {
		fprintf(stderr, "Not RTM_NEWNEIGH: %08x %08x %08x\n",
			n->nlmsg_len, n->nlmsg_type, n->nlmsg_flags);

		return 0;
	}
	len -= NLMSG_LENGTH(sizeof(*r));
	if (len < 0) {
		fprintf(stderr, "BUG: wrong nlmsg len %d\n", len);
		return -1;
	}

	if (filter.flushb && n->nlmsg_type != RTM_NEWNEIGH)
		return 0;

	if (filter.family && filter.family != r->ndm_family)
		return 0;
	if (filter.index && filter.index != r->ndm_ifindex)
		return 0;
	if (!(filter.state&r->ndm_state) &&
	    !(r->ndm_flags & NTF_PROXY) &&
	    (r->ndm_state || !(filter.state&0x100)) &&
	     (r->ndm_family != AF_DECnet))
		return 0;

	if (filter.master && !(n->nlmsg_flags & NLM_F_DUMP_FILTERED)) {
			fprintf(fp, "\nWARNING: Kernel does not support filtering by master device\n\n");
	}

	parse_rtattr(tb, NDA_MAX, NDA_RTA(r), n->nlmsg_len - NLMSG_LENGTH(sizeof(*r)));

	if (tb[NDA_DST]) {
		if (filter.pfx.family) {
			inet_prefix dst = { .family = r->ndm_family };

			memcpy(&dst.data, RTA_DATA(tb[NDA_DST]), RTA_PAYLOAD(tb[NDA_DST]));
			if (inet_addr_match(&dst, &filter.pfx, filter.pfx.bitlen))
				return 0;
		}
	}
	if (filter.unused_only && tb[NDA_CACHEINFO]) {
		struct nda_cacheinfo *ci = RTA_DATA(tb[NDA_CACHEINFO]);

		if (ci->ndm_refcnt)
			return 0;
	}

	if (filter.flushb) {
		struct nlmsghdr *fn;

		if (NLMSG_ALIGN(filter.flushp) + n->nlmsg_len > filter.flushe) {
			if (flush_update())
				return -1;
		}
		fn = (struct nlmsghdr *)(filter.flushb + NLMSG_ALIGN(filter.flushp));
		memcpy(fn, n, n->nlmsg_len);
		fn->nlmsg_type = RTM_DELNEIGH;
		fn->nlmsg_flags = NLM_F_REQUEST;
		fn->nlmsg_seq = ++rth.seq;
		filter.flushp = (((char *)fn) + n->nlmsg_len) - filter.flushb;
		filter.flushed++;
		if (show_stats < 2)
			return 0;
	}

	if (n->nlmsg_type == RTM_DELNEIGH)
		fprintf(fp, "Deleted ");
	else if (n->nlmsg_type == RTM_GETNEIGH)
		fprintf(fp, "miss ");
	if (tb[NDA_DST]) {
		fprintf(fp, "%s ",
			format_host_rta(r->ndm_family, tb[NDA_DST]));
	}
	if (!filter.index && r->ndm_ifindex)
		fprintf(fp, "dev %s ", ll_index_to_name(r->ndm_ifindex));
	if (tb[NDA_LLADDR]) {
		SPRINT_BUF(b1);
		fprintf(fp, "lladdr %s", ll_addr_n2a(RTA_DATA(tb[NDA_LLADDR]),
					      RTA_PAYLOAD(tb[NDA_LLADDR]),
					      ll_index_to_type(r->ndm_ifindex),
					      b1, sizeof(b1)));
	}
	if (r->ndm_flags & NTF_ROUTER) {
		fprintf(fp, " router");
	}
	if (r->ndm_flags & NTF_PROXY) {
		fprintf(fp, " proxy");
	}
	if (tb[NDA_CACHEINFO] && show_stats) {
		struct nda_cacheinfo *ci = RTA_DATA(tb[NDA_CACHEINFO]);
		int hz = get_user_hz();

		if (ci->ndm_refcnt)
			printf(" ref %d", ci->ndm_refcnt);
		fprintf(fp, " used %d/%d/%d", ci->ndm_used/hz,
		       ci->ndm_confirmed/hz, ci->ndm_updated/hz);
	}

	if (tb[NDA_PROBES] && show_stats) {
		__u32 p = rta_getattr_u32(tb[NDA_PROBES]);

		fprintf(fp, " probes %u", p);
	}

	if (r->ndm_state) {
		int nud = r->ndm_state;

		fprintf(fp, " ");

#define PRINT_FLAG(f) if (nud & NUD_##f) { \
	nud &= ~NUD_##f; fprintf(fp, #f "%s", nud ? "," : ""); }
		PRINT_FLAG(INCOMPLETE);
		PRINT_FLAG(REACHABLE);
		PRINT_FLAG(STALE);
		PRINT_FLAG(DELAY);
		PRINT_FLAG(PROBE);
		PRINT_FLAG(FAILED);
		PRINT_FLAG(NOARP);
		PRINT_FLAG(PERMANENT);
#undef PRINT_FLAG
	}
	fprintf(fp, "\n");

	fflush(fp);
	return 0;
}

void ipneigh_reset_filter(int ifindex)
{
	memset(&filter, 0, sizeof(filter));
	filter.state = ~0;
	filter.index = ifindex;
}

static int do_show_or_flush(FILE *mypipe)
{
	struct {
		struct nlmsghdr	n;
		struct ndmsg		ndm;
		char			buf[256];
	} req = {
		.n.nlmsg_type = RTM_GETNEIGH,
		.n.nlmsg_len = NLMSG_LENGTH(sizeof(struct ndmsg)),
	};
	static int preferred_family = AF_UNSPEC;
	ipneigh_reset_filter(0);
	if (!filter.family)
		filter.family = preferred_family;
	filter.state = 0xFF & ~NUD_NOARP;

	// RTM_GETLINK forbidden by Android API 30
	//ll_init_map(&rth);
	req.ndm.ndm_family = filter.family;
	if (rtnl_dump_request_n(&rth, &req.n) < 0) {
		perror("Cannot send dump request");
		exit(1);
	}
	if (rtnl_dump_filter(&rth, print_neigh, mypipe) < 0) {
		fprintf(stderr, "Dump terminated\n");
		exit(1);
	}
	return 0;
}

int Java_com_aaronjwood_portauthority_async_ScanHostsAsyncTask_nativeIPNeigh(JNIEnv* env,
                                                  jobject thiz,
												  jint fileDescriptor) {

	FILE *mypipe = fdopen(fileDescriptor, "w");
	if (mypipe == NULL) {
		perror("Cannot fdopen");
		exit(EXIT_FAILURE);
	}

	if (rtnl_open(&rth, 0) < 0)
		exit(1);

	int res = do_show_or_flush(mypipe);

	rtnl_close(&rth);
	fclose(mypipe);

	return res;
}
