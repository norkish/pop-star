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

#load target_empath

with open(target_empath) as f:
    content = f.readlines()

target_id = content[0]
target_empath_vec = eval(content[1]).values()

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
