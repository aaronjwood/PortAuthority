#ifndef __LIBNETLINK_H__
#define __LIBNETLINK_H__ 1

#include <stdio.h>
#include <string.h>
#include <asm/types.h>
#include <linux/netlink.h>
#include <linux/rtnetlink.h>
#include <linux/if_link.h>
#include <linux/if_addr.h>
#include <linux/neighbour.h>
#include <linux/netconf.h>
#include <arpa/inet.h>

struct rtnl_handle {
	int			fd;
	struct sockaddr_nl	local;
	struct sockaddr_nl	peer;
	__u32			seq;
	__u32			dump;
	int			proto;
	FILE		       *dump_fp;
#define RTNL_HANDLE_F_LISTEN_ALL_NSID		0x01
#define RTNL_HANDLE_F_SUPPRESS_NLERR		0x02
	int			flags;
};

struct nlmsg_list {
	struct nlmsg_list *next;
	struct nlmsghdr   h;
};

struct nlmsg_chain {
	struct nlmsg_list *head;
	struct nlmsg_list *tail;
};

extern int rcvbuf;

int rtnl_open(struct rtnl_handle *rth, unsigned int subscriptions)
	__attribute__((warn_unused_result));

int rtnl_open_byproto(struct rtnl_handle *rth, unsigned int subscriptions,
			     int protocol)
	__attribute__((warn_unused_result));

void rtnl_close(struct rtnl_handle *rth);

typedef int (*req_filter_fn_t)(struct nlmsghdr *nlh, int reqlen);

int rtnl_dump_request_n(struct rtnl_handle *rth, struct nlmsghdr *n)
	__attribute__((warn_unused_result));

struct rtnl_ctrl_data {
	int	nsid;
};

typedef int (*rtnl_filter_t)(const struct sockaddr_nl *,
			     struct nlmsghdr *n, void *);

typedef int (*nl_ext_ack_fn_t)(const char *errmsg, uint32_t off,
			       const struct nlmsghdr *inner_nlh);

struct rtnl_dump_filter_arg {
	rtnl_filter_t filter;
	void *arg1;
	__u16 nc_flags;
};

int rtnl_dump_filter_l(struct rtnl_handle *rth,
			      const struct rtnl_dump_filter_arg *arg);
int rtnl_dump_filter_nc(struct rtnl_handle *rth,
			rtnl_filter_t filter,
			void *arg, __u16 nc_flags);
#define rtnl_dump_filter(rth, filter, arg) \
	rtnl_dump_filter_nc(rth, filter, arg, 0)
int rtnl_send_check(struct rtnl_handle *rth, const void *buf, int)
	__attribute__((warn_unused_result));

int addattr32(struct nlmsghdr *n, int maxlen, int type, __u32 data);
int addattr_l(struct nlmsghdr *n, int maxlen, int type,
	      const void *data, int alen);

int parse_rtattr(struct rtattr *tb[], int max, struct rtattr *rta, int len);
int parse_rtattr_flags(struct rtattr *tb[], int max, struct rtattr *rta,
			      int len, unsigned short flags);

#define RTA_TAIL(rta) \
		((struct rtattr *) (((void *) (rta)) + \
				    RTA_ALIGN((rta)->rta_len)))

#define parse_rtattr_nested(tb, max, rta) \
	(parse_rtattr((tb), (max), RTA_DATA(rta), RTA_PAYLOAD(rta)))

static inline __u8 rta_getattr_u8(const struct rtattr *rta)
{
	return *(__u8 *)RTA_DATA(rta);
}
static inline __u16 rta_getattr_u16(const struct rtattr *rta)
{
	return *(__u16 *)RTA_DATA(rta);
}
static inline __be16 rta_getattr_be16(const struct rtattr *rta)
{
	return ntohs(rta_getattr_u16(rta));
}
static inline __u32 rta_getattr_u32(const struct rtattr *rta)
{
	return *(__u32 *)RTA_DATA(rta);
}
static inline __be32 rta_getattr_be32(const struct rtattr *rta)
{
	return ntohl(rta_getattr_u32(rta));
}
static inline __u64 rta_getattr_u64(const struct rtattr *rta)
{
	__u64 tmp;

	memcpy(&tmp, RTA_DATA(rta), sizeof(__u64));
	return tmp;
}
static inline const char *rta_getattr_str(const struct rtattr *rta)
{
	return (const char *)RTA_DATA(rta);
}

#define NLMSG_TAIL(nmsg) \
	((struct rtattr *) (((void *) (nmsg)) + NLMSG_ALIGN((nmsg)->nlmsg_len)))

#ifndef IFA_RTA
#define IFA_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct ifaddrmsg))))
#endif
#ifndef IFA_PAYLOAD
#define IFA_PAYLOAD(n)	NLMSG_PAYLOAD(n, sizeof(struct ifaddrmsg))
#endif

#ifndef IFLA_RTA
#define IFLA_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct ifinfomsg))))
#endif
#ifndef IFLA_PAYLOAD
#define IFLA_PAYLOAD(n)	NLMSG_PAYLOAD(n, sizeof(struct ifinfomsg))
#endif

#ifndef NDA_RTA
#define NDA_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct ndmsg))))
#endif
#ifndef NDA_PAYLOAD
#define NDA_PAYLOAD(n)	NLMSG_PAYLOAD(n, sizeof(struct ndmsg))
#endif

#ifndef NDTA_RTA
#define NDTA_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct ndtmsg))))
#endif
#ifndef NDTA_PAYLOAD
#define NDTA_PAYLOAD(n) NLMSG_PAYLOAD(n, sizeof(struct ndtmsg))
#endif

#ifndef NETNS_RTA
#define NETNS_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct rtgenmsg))))
#endif
#ifndef NETNS_PAYLOAD
#define NETNS_PAYLOAD(n)	NLMSG_PAYLOAD(n, sizeof(struct rtgenmsg))
#endif

#ifndef IFLA_STATS_RTA
#define IFLA_STATS_RTA(r) \
	((struct rtattr *)(((char *)(r)) + NLMSG_ALIGN(sizeof(struct if_stats_msg))))
#endif

/* User defined nlmsg_type which is used mostly for logging netlink
 * messages from dump file */
#define NLMSG_TSTAMP	15

#endif /* __LIBNETLINK_H__ */
