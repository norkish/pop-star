from empath import Empath
import csv
from HTMLParser import HTMLParser
import sys
lexicon = Empath()

import re

reload(sys)
sys.setdefaultencoding('utf8')

def cleanhtml(raw_html):
  cleanr = re.compile('<.*?>')
  cleantext = re.sub(cleanr, '', raw_html)
  return cleantext.replace("&","and")

lyric_filename = sys.argv[1]
h = HTMLParser()
with open(lyric_filename, "r") as f:
    reader = csv.reader(f, delimiter=",")
    for i, line in enumerate(reader):
        if i == 0:
            line.append('empath_vec')
            print line
        else:
            line[3]=cleanhtml(h.unescape(line[3]).encode('utf-8'))
            line.append(lexicon.analyze(line[3], normalize=True))
            print line
