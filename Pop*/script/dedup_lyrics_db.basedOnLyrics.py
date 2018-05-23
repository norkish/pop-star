from sets import Set
import re
import sys


key_dict = Set() 

pattern = re.compile('[^0-9a-zA-Z]+')
with open(sys.argv[1]) as f:
    for line in f:
        entry = eval(line)
        lyric = pattern.sub('',entry[3].lower())[:100]
        if len(lyric) == 0:
            continue
        if lyric in key_dict:
            continue
        else:
            key_dict.add(lyric)

        print line.strip()
