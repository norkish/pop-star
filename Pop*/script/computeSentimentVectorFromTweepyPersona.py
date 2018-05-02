import tweepy
import time
from empath import Empath
import sys
import datetime
lexicon = Empath()

if len(sys.argv) < 2:
    print "Please provide persona file"
    sys.exit(-1)

access_token = "991028933465333760-aiqTPSf873rJHWlOaSydfLusTy0tuFS"
access_token_secret = "dZkwEKV1rLlGswDXBQcYcikCStm0nY0RVDMyWde4Wtm4j"
consumer_key = "9RZd6SYUjsqiJkvDFV6IgyRQI"
consumer_secret = "bvVCZY9HRRZ5qlIXDxiEnJPA2GTfzzOfwQMI7pjSDKlspKmQd3"

auth = tweepy.OAuthHandler(consumer_key,consumer_secret)
auth.set_access_token(access_token, access_token_secret)

api = tweepy.API(auth,wait_on_rate_limit=True)

#Read in a list of interests with a weight for interest
interests_and_weights = eval(open(sys.argv[1],'r').read())

bssf_tweet = None
bssf_empath = None
bssf_score = -1.0
since_date=datetime.date.today()-datetime.timedelta(days=3)

def emotional_score(empath_vec):
    return sum(v for k,v in empath_vec.iteritems() if k in ["joy","surprise","anger","sadness","fear","disgust","positive_emotion","negative_emotion","optimism","hate","envy","love","lust","shame","disappointment","timidity"])

#For each interest i,
for i,w in interests_and_weights.iteritems():
    max_tweets = 1000
    #Search the latest tweets t_i related to i
    searched_tweets = [status for status in tweepy.Cursor(api.search, q=i + "  -filter:retweets", since=since_date,languages=["en"]).items(max_tweets)]

    for idx,tweet in enumerate(searched_tweets):
        #print idx,tweet.text
        #For each tweet t_i, compute the empath vector
        empath_vec = lexicon.analyze(tweet.text, normalize=True)
        #print empath_vec
        score = emotional_score(empath_vec) 
        #print "Score:",score,"*",w,"=",(score*w)
        score *= w
        if score > bssf_score:
            bssf_tweet = tweet
            bssf_tweet.persona_interest = i
            bssf_score = score
            bssf_empath = empath_vec

    break

#Report the interest i, tweet t_i, and empath vector v_t_i with the highest emotional value weighted by the interest weight w_i
ts = bssf_tweet.created_at.strftime('%A, %B %d, %Y %I:%M %p')
print bssf_tweet.persona_interest, bssf_tweet.user.name,ts,bssf_tweet.text.encode('utf-8'),bssf_score
print bssf_empath
