README FOR STRUCTURE ANNOTATION

Each file represents the structure for a different song.

The file is organized by segment. 

==SEGMENT DEFINITION==

The first line in each segment has the following format:

SEGMENT_LABEL	MEASURE	MEASURE_REPEAT	LYRIC(or beat offset)	DELTA_FROM_FORM_START	<OCCURRENCE>

SEGMENT_LABEL can be any of INTRO, VERSE, PRECHORUS, CHORUS, BRIDGE, OUTRO, INTERLUDE;

MEASURE is the 1-based measure number as shown in the mxl file in a file viewer.

MEASURE_REPEAT is the 1-based number representing which iteration of the measure is being referenced. For example, if it is the first time the measure has been seen (e.g., the first verse), this value would be 1. If the measure is only seen once, the value is 1. This makes it very easy to annotate multiple choruses that simply repeat measures by simply copying and pasting the annotations from the previous verse and incrementing the MEASURE_REPEAT value.

LYRIC can be used to specify a lyric in the measure that represents the segment starting boundary. Alternatively a 0-based decimal can be given representing the offset into the measure.

The DELTA_FROM_FORM_START is used to denote where the downbeat of the segment form actually begins relative to the MEASURE (for example if the prev defined token is a pickup or offset from the segment downbeat). This allows the annotator to annotate segments where, e.g., there are pickup notes with lyrics that belong in the chorus, but that begin before the actual downbeat of the chorus. If the specified token occurs within the first full measure of the form, the value is 0. Pickups would be -1 (or e.g., -2 if more than a measure of pickups). Full measure of rest would make this value 1, etc. 

OCCURRENCE is optional and should be specified when the LYRIC value appears twice in the measure defined by MEASURE. Both MEASURE and OCCURRENCE are 1-based values. DELTA_FROM_FORM_START is 0 if beat specified by MEASURE and LYRIC appear in the first full bar of the form. The occurrences count is repeat (i.e., verse) dependent.

Segments should be listed in order starting with the first segment in the song. Be sure that the first segment start coincides with the beginning of the song. For example,

INTRO	1	1	0.0	0

==CONSTRAINT DEFINITION==

The segment definition line is to be followed by an empty line, after which one or more constraints are defined, each defined as follows (and followed by an empty line):

constraint_type
LYRIC	MEASURE	MEASURE_REPEAT
LYRIC	MEASURE	MEASURE_REPEAT

constraint_types include rhyme or exactBinaryMatch. For rhyme, each of the LYRIC lines below it reference a syllable within a common rhyme group, each with its MEASURE and MEASURE_REPEAT as defined above. There must 2 or more LYRIC lines for a rhyme constraint, all of which occur within the segment boundaries. For example, 

rhyme
hurt	42	1
sert	43	1

for exactBinaryMatch, the first LYRIC denotes the first lyric and a second LYRIC line denotes the last lyric in a sequence of lyrics that are intentional identical sequences of lyrics with other sequences in other segments. The sequence of lyrics itself will be a unique identifier for the sequence. This constraint is not currently doing anything. Repetitive lyrics across choruses should be marked with this constraint. For example,

exactBinaryMatch
You	48	1
friend	34	3

==COMMENTS==

Comments can be made by prefacing the line with //. For example,

VERSE	38	2.If	-1
// missing lyrics

==MISCELLANEOUS==

Note that rhyme schemes should be contained within a segment. This may require adjusting how segments are defined. For example, a tagline chorus is different from a chorus in that it is part of the verse (which repeats across verse instances). It is therefore part of the rhyme scheme for the verse and therefore included as part of the verse.


==KNOWN PROBLEMS==

1. If lyrics are missing, it creates structure templates with no constraints. Solution: if lyrics are expected for the segment type and no lyric constraints are found (or the lyric count is below some threshold), don't train the model on the template.

2. Legitimate templates with no constraints (e.g., INTRO, INTERLUDE, etc.) can be sampled for other segment types (e.g., CHORUS) resulting in a lyrical segment with no constraints. This is high priority. Solution: structure should be segment dependent.