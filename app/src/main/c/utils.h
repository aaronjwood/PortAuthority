#ifndef __UTILS_H__
#define __UTILS_H__ 1

#include <asm/types.h>
#include <stdio.h>

// from if.h
//#define	IFNAMSIZ	16

// Needed for Android build, why ?
#define AF_MPLS   28

// from if_arp.h
#define ARPHRD_VOID	  0xFFFF	/* Void type, nothing is known */
#define ARPHRD_TUNNEL	768		/* IPIP tunnel			*/
#define ARPHRD_TUNNEL6	769		/* IP6IP6 tunnel       		*/
#define ARPHRD_IPGRE	778		/* GRE over IP			*/
#define ARPHRD_SIT	776		/* sit0 device - IPv6-in-IPv4	*/


// from utils.h
#define SPRINT_BSIZE 64
#define SPRINT_BUF(x)	char x[SPRINT_BSIZE]
#define format_host_rta(af, rta) \
	format_host(af, RTA_PAYLOAD(rta), RTA_DATA(rta))
#define DN_MAXADDL 20
#define IPX_NODE_LEN 6

struct ipx_addr {
	u_int32_t ipx_net;
	u_int8_t  ipx_node[IPX_NODE_LEN];
};

struct dn_naddr
{
        unsigned short          a_len;
        unsigned char a_addr[DN_MAXADDL];
};

typedef struct
{
	__u16 flags;
	__u16 bytelen;
	__s16 bitlen;
	/* These next two fields match rtvia */
	__u16 family;
	__u32 data[64];
} inet_prefix;

int get_user_hz(void);
const char *ipx_ntop(int af, const void *addr, char *str, size_t len);
const char *dnet_ntop(int af, const void *addr, char *str, size_t len);
const char *mpls_ntop(int af, const void *addr, char *buf, size_t buflen);
int inet_addr_match(const inet_prefix *a, const inet_prefix *b, int bits);
void ipneigh_reset_filter(int ifindex);
const char *rt_addr_n2a_r(int af, int len, const void *addr,
			       char *buf, int buflen);
const char *format_host(int af, int len, const void *addr);
const char *format_host_r(int af, int len, const void *addr,
			       char *buf, int buflen);
const char *ll_addr_n2a(const unsigned char *addr, int alen,
			int type, char *buf, int blen);

#endif /* __UTILS_H__ */