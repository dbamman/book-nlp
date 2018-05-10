import sys
from bs4 import BeautifulSoup

def proc(filenames):
	for filename in filenames:
		page=""
		file=open(filename)
		for line in file:
			page+=line.rstrip() + " "
		file.close()

		invalid={}
		invalid["00"]=1
		invalid["01"]=1
		invalid["02"]=1
		invalid["44"]=1
				
		soup=BeautifulSoup(page, "html5lib")
		ps=soup.findAll('p')
		for p in ps:
			ss=p.findAll('s')
			for s in ss:
				ws=s.findChildren()
				for child in ws:
					if child.name == "punc":
						print("%s\t%s\t%s\t%s\t%s\t%s" % (child.text, "_", "_", "_", "_", "O"))
					else:
						lex=None
						wnsn=None
						pos=None
						lemma=None
						if "lemma" in child.attrs:
							lemma=child.attrs["lemma"]
						if "pos" in child.attrs:
							pos=child.attrs["pos"]
						
						if "lexsn" in child.attrs:
							lex=child.attrs["lexsn"]
							wnsn=child.attrs["wnsn"]
						
						parts=child.text.split("_")
						if lex == None:
							lex="O"

						modlex=lex
						if lex != None and lex != "O":
							lexparts=lex.split(":")
							modlex=lexparts[1]
						if modlex in invalid:
							lex="O"

						if modlex == "03":
							if lemma == "person":
								modlex="18"
							elif lemma == "group":
								modlex="14"
							elif lemma == "location":
								modlex="15"
							elif lemma == "food":
								modlex="13"
							elif lemma == "plant":
								modlex="20"
							elif lemma == "animal":
								modlex="05"
							elif lemma == "time":
								modlex="28"
							elif lemma == "event":
								modlex="11"
							elif lemma == "state":
								modlex="26"

						if lex == "O":
							if len(parts) == 1:
								print("%s\t%s\t%s\t%s\t%s\t%s" % (child.text, lex, pos, lemma, wnsn, lex))
							else:
								for i in range(len(parts)):
									print("%s\t%s\t%s\t%s\t%s\t%s" % (parts[i], lex, pos, lemma, wnsn, lex))


						elif len(parts) == 1:
							print("%s\t%s\t%s\t%s\t%s\t%s-%s" % (child.text, lex, pos, lemma, wnsn, "B", modlex))
						else:
							print("%s\t%s\t%s\t%s\t%s\t%s-%s" % (parts[0], lex, pos, lemma, wnsn, "B", modlex))
							for i in range(1,len(parts)):
								print("%s\t%s\t%s\t%s\t%s\t%s-%s" % (parts[i], lex, pos, lemma, wnsn, "I", modlex))
				
			print()
		print()

# Use to parse the Semcor 3.0 data as preprocessing for supersense tagging
# python parseSemcor.py semcor3.0/brown1/tagfiles/* semcor3.0/brown2/tagfiles/* > all.brown.txt
proc(sys.argv[1:])