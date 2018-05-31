import numpy as np
import unidecode
from HTMLParser import HTMLParser
import sys

target_empath = sys.argv[1]
lyrics_empath = sys.argv[2]
count = -1
if len(sys.argv) > 3:
    count = int(sys.argv[3])
else:
    count = 10

tweet_idx = 0
if len(sys.argv) > 4:
    tweet_idx = int(sys.argv[4])

#load target_empath

with open(target_empath) as f:
    content = f.readlines()

target_id = content[tweet_idx*2]
target_empath_vec = eval(content[tweet_idx*2+1]).values()

#distill the target_empath
t = sorted(range(len(target_empath_vec)), key=lambda i: target_empath_vec[i])[-2:]

for idx in range(len(target_empath_vec)):
    if idx not in t and target_empath_vec[idx] != target_empath_vec[t[-1]]:
        target_empath_vec[idx] = 0.0

#load lyrics_empath

topN = [None]*count

with open(lyrics_empath) as f:
    for line in f:
        entry = eval(line)
        if entry[0] == 'url':
            continue
        if entry[5] is None:
#            print "LINE IS NONETYPE:"
#            print line
            continue

        lyrics_empath_vec = entry[5].values()

        #for the target_empath, compute distance of vector from all lyrics_emptah_vectors
        dist = (np.asarray(lyrics_empath_vec) - np.asarray(target_empath_vec))**2
        dist = np.sum(dist)
        dist = np.sqrt(dist)

        if topN[-1] == None or dist < topN[-1][6]:
            for i,topI in enumerate(topN):
                if topI == None or dist < topI[6]:
                    entry.append(dist)
                    topN.insert(i,entry)
                    topN.pop()
                    break

h = HTMLParser()

for x in topN:
    x[1] = unidecode.unidecode(h.unescape(x[1]).decode('utf-8'))
    x[3] = unidecode.unidecode(h.unescape(x[3]).decode('utf-8'))
    for i,val in enumerate(x):
        if i != 0:
            print "[[:DELIMITER:]]",
        print repr(val),
    print
