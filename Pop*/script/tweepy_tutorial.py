import tweepy

access_token = "991028933465333760-aiqTPSf873rJHWlOaSydfLusTy0tuFS"
access_token_secret = "dZkwEKV1rLlGswDXBQcYcikCStm0nY0RVDMyWde4Wtm4j"
consumer_key = "9RZd6SYUjsqiJkvDFV6IgyRQI"
consumer_secret = "bvVCZY9HRRZ5qlIXDxiEnJPA2GTfzzOfwQMI7pjSDKlspKmQd3"

auth = tweepy.OAuthHandler(consumer_key, consumer_secret)
auth.set_access_token(access_token, access_token_secret)

api = tweepy.API(auth)

public_tweets = api.home_timeline()
for tweet in public_tweets:
    print tweet.text
