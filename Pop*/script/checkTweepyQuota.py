import tweepy
import datetime

access_token = "991028933465333760-aiqTPSf873rJHWlOaSydfLusTy0tuFS"
access_token_secret = "dZkwEKV1rLlGswDXBQcYcikCStm0nY0RVDMyWde4Wtm4j"
consumer_key = "Dg330ThbReo4ctTovgJDKPJlQ"
consumer_secret = "cHtRVVWRaxw8gEDWW1HygNORV9Byc5Mhrlvzz2qtfbFkNZZzpA"

auth = tweepy.OAuthHandler(consumer_key,consumer_secret)
auth.set_access_token(access_token, access_token_secret)

api = tweepy.API(auth)
data = api.rate_limit_status()

print data['resources']['search']['/search/tweets']['remaining'],"requests remaining of",
print data['resources']['search']['/search/tweets']['limit']
print "Time to next reset:",datetime.datetime.fromtimestamp(data['resources']['search']['/search/tweets']['reset']).strftime('%Y-%m-%d %H:%M:%S')
