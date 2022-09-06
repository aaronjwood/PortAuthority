/*
 * ll_map.c
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
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <net/if.h>

#include "libnetlink.h"
#include "ll_map.h"
#include "list.h"

struct ll_cache {
	struct hlist_node idx_hash;
	unsigned 	index;
	unsigned short	type;
	char		name[];
};

#define IDXMAP_SIZE	1024
static struct hlist_head idx_head[IDXMAP_SIZE];

static struct ll_cache *ll_get_by_index(unsigned index)
{
	struct hlist_node *n;
	unsigned h = index & (IDXMAP_SIZE - 1);

	hlist_for_each(n, &idx_head[h]) {
		struct ll_cache *im
			= container_of(n, struct ll_cache, idx_hash);
		if (im->index == index)
			return im;
	}

	return NULL;
}

const char *ll_idx_n2a(unsigned idx, char *buf)
{
	const struct ll_cache *im;

	if (idx == 0)
		return "*";

	im = ll_get_by_index(idx);
	if (im)
		return im->name;

	if (if_indextoname(idx, buf) == NULL)
		snprintf(buf, IFNAMSIZ, "if%d", idx);

	return buf;
}

const char *ll_index_to_name(unsigned idx)
{
	static char nbuf[IFNAMSIZ];

	return ll_idx_n2a(idx, nbuf);
}

int ll_index_to_type(unsigned idx)
{
	const struct ll_cache *im;

	if (idx == 0)
		return -1;

	im = ll_get_by_index(idx);
	return im ? im->type : -1;
}


