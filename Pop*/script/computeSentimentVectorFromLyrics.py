from empath import Empath
import sys
lexicon = Empath()

lyric_filename = sys.argv[1]

with open(lyric_filename) as f:
    content = f.readlines()
# you may also want to remove whitespace characters like `\n` at the end of each line
content = [x.strip() for x in content] 

i = 0
while (i < len(content)):
    print content[i]
    i+=1
    print lexicon.analyze(content[i], normalize=True)
    i+=1
