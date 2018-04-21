import numpy as np
import sys

target_empath = sys.argv[1]
lyrics_empath = sys.argv[2]
count = -1
if len(sys.argv) > 3:
    count = int(sys.argv[3])

#load target_empath

with open(target_empath) as f:
    content = f.readlines()

target_id = content[0]
target_empath_vec = eval(content[1]).values()

#load lyrics_empath

with open(lyrics_empath) as f:
    content = f.readlines()

i = 0
lyrics_id = (len(content)/2)*[None]
lyrics_empath_vec = (len(content)/2)*[None]
while i < len(content):
    lyrics_id[i/2] = content[i].strip()
    i+=1
    lyrics_empath_vec[i/2] = eval(content[i]).values()
    i+=1

#for the target_empath, compute distance of vector from all lyrics_emptah_vectors
dist = (np.asarray(lyrics_empath_vec) - np.asarray(target_empath_vec))**2
dist = np.sum(dist, axis=1)
dist = np.sqrt(dist)

combined = zip(lyrics_id,dist, lyrics_empath_vec)
combined.sort(lambda x,y: cmp(x[1],y[1]))

if count != -1:
    combined = combined[:count]

for x in combined:
    print x[1],x[0]
