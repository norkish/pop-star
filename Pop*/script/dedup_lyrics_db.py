from sets import Set
import re
import sys


artist_song_dict = {}

pattern = re.compile('[\W_]+')
with open(sys.argv[1]) as f:
    for line in f:
        entry = eval(line)
        if entry[5] == None:
            continue
        title = pattern.sub('',entry[1].lower())
        artist = pattern.sub('',entry[4].lower())
        if len(title) == 0 or len(artist) == 0:
            continue
        if artist in artist_song_dict:
            found_match = False
            for o_title in artist_song_dict[artist]:
                sub_len = min(len(o_title),len(title))
                if o_title[:sub_len] == title[:sub_len]:
                    found_match = True
                    break

            if found_match:
                continue
            else:
                artist_song_dict[artist].add(title)
        else:
            artist_song_dict[artist] = Set([title])

        print line.strip()
