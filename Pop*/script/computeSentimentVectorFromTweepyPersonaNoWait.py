import tweepy
from HTMLParser import HTMLParser
import time
from empath import Empath
import random
import sys
import datetime
lexicon = Empath()

if len(sys.argv) < 2:
    print "Please provide persona file"
    sys.exit(-1)

access_token = "991028933465333760-aiqTPSf873rJHWlOaSydfLusTy0tuFS"
access_token_secret = "dZkwEKV1rLlGswDXBQcYcikCStm0nY0RVDMyWde4Wtm4j"
consumer_key = "Dg330ThbReo4ctTovgJDKPJlQ"
consumer_secret = "cHtRVVWRaxw8gEDWW1HygNORV9Byc5Mhrlvzz2qtfbFkNZZzpA"

auth = tweepy.OAuthHandler(consumer_key,consumer_secret)
auth.set_access_token(access_token, access_token_secret)

api = tweepy.API(auth)

#Read in a list of interests with a weight for interest
interests_and_weights = eval(open(sys.argv[1],'r').read())

bssf_tweet = None
bssf_empath = None
bssf_score = -1.0
since_date=datetime.date.today()-datetime.timedelta(days=1)

def emotional_score(empath_vec):
    return sum(v for k,v in empath_vec.iteritems() if k in ["joy","surprise","anger","sadness","fear","disgust","positive_emotion","negative_emotion","optimism","hate","envy","love","lust","shame","disappointment","timidity"])

data = api.rate_limit_status()

#print "Statuses:"
#print data['resources']['statuses']['/statuses/home_timeline']
#print data['resources']['users']['/users/lookup']

count = -1
if len(sys.argv) > 3:
    count = int(sys.argv[2])
else:
    count = 10

topN = [None]*count

#For each interest i,
for i,w in interests_and_weights.iteritems():
    max_tweets = 1000
    #Search the latest tweets t_i related to i
    searched_tweets = [status for status in tweepy.Cursor(api.search, q=i + "  -filter:retweets", since=since_date,languages=["en"],tweet_mode='extended').items(max_tweets)]

    for idx,tweet in enumerate(searched_tweets):
        if len(tweet.full_text) < 50:
            continue
        #print idx,tweet.text
        #For each tweet t_i, compute the empath vector
        empath_vec = lexicon.analyze(tweet.full_text, normalize=True)
        #print empath_vec
        score = emotional_score(empath_vec) 
        #print "Score:",score,"*",w,"=",(score*w)
        score *= w
        for j,topI in enumerate(topN):
            if topI == None or score > topI[1]:
                tweet.persona_interest = i
                topN.insert(j,[tweet,score,empath_vec])
                topN.pop()
                break;

#Report the interest i, tweet t_i, and empath vector v_t_i with the highest emotional value weighted by the interest weight w_i
for j,topI in enumerate(topN):
    bssf_tweet = topI[0]
    ts = bssf_tweet.created_at.strftime('%A, %B %d, %Y at %I:%M %p')
    h = HTMLParser()
    print "SEARCH_KEYWORD:",bssf_tweet.persona_interest, "USERNAME:", h.unescape(bssf_tweet.user.name).encode('utf-8'),"TIMESTAMP:",ts,"TWEETBODY:",h.unescape(bssf_tweet.full_text).encode('utf-8'),"MATCH_SCORE:",topI[1]
    print topI[2]
