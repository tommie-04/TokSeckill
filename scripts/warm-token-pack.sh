#!/usr/bin/env bash
# Load pack windows into Redis and initialize each stock key once.
set -euo pipefail

if (( $# == 0 )); then
    echo "Usage: $0 PACK_ID [PACK_ID ...]" >&2
    exit 2
fi

mysql_args=(--host="${MYSQL_HOST:-127.0.0.1}" --port="${MYSQL_PORT:-3306}"
            --user="${MYSQL_USER:-root}" --database="${MYSQL_DATABASE:-tokenflash}" --batch --skip-column-names)
redis_args=(-h "${REDIS_HOST:-127.0.0.1}" -p "${REDIS_PORT:-6379}" -n "${REDIS_DATABASE:-0}")

if [[ -n "${REDIS_PASSWORD:-}" ]]; then
    export REDISCLI_AUTH="$REDIS_PASSWORD"
fi
for pack_id in "$@"; do
    if [[ ! "$pack_id" =~ ^[1-9][0-9]*$ ]]; then
        echo "Invalid pack ID: $pack_id" >&2
        exit 2
    fi
    row=$(MYSQL_PWD="${MYSQL_PASSWORD:-}" mysql "${mysql_args[@]}" -e "
        SELECT s.stock, UNIX_TIMESTAMP(s.begin_time), UNIX_TIMESTAMP(s.end_time)
        FROM tb_seckill_voucher s JOIN tb_voucher v ON s.voucher_id = v.id
        WHERE v.id = $pack_id AND v.type = 1 AND v.status = 1 AND v.token_units > 0")
    read -r stock begin end <<< "$row"
    if [[ ! "$stock" =~ ^[0-9]+$ || ! "$begin" =~ ^[0-9]+$ || ! "$end" =~ ^[0-9]+$ || "$begin" -ge "$end" ]]; then
        echo "No active catalog record with a valid sale window for pack $pack_id" >&2
        exit 1
    fi
    warmed=$(redis-cli --raw "${redis_args[@]}" EVAL "
        redis.call('SET', KEYS[1], ARGV[1], 'NX')
        redis.call('HSET', KEYS[2], 'begin', ARGV[2], 'end', ARGV[3])
        return 'OK'
    " 2 "seckill:stock:$pack_id" "seckill:window:$pack_id" "$stock" "$begin" "$end")
    if [[ "$warmed" != OK ]]; then
        echo "Redis warmup failed for pack $pack_id: $warmed" >&2
        exit 1
    fi
    echo "Warmed pack $pack_id"
done
