-- KEYS: Stock counter, reserved users, sale window. ARGV: user ID, current epoch second.
local now = tonumber(ARGV[2])
local window = redis.call('HMGET', KEYS[3], 'begin', 'end')
local startAt = tonumber(window[1])
local endAt = tonumber(window[2])
if not startAt or not endAt or now < startAt or now >= endAt then
    return 3
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 2
end
local stock = tonumber(redis.call('GET', KEYS[1]))
if not stock or stock <= 0 then
    return 1
end
redis.call('DECR', KEYS[1])
redis.call('SADD', KEYS[2], ARGV[1])
return 0
