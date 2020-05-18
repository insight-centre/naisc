package org.insightcentre.uld.naisc.feature.wordnet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import joptsimple.internal.Strings;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * The contents of a wordnet
 * @author John McCrae
 */
public class WordNetData {
    private final HashMap<String, LexicalEntry> entriesById = new HashMap<>();
    private final WordNetEntryTrie entriesByLemma = new WordNetEntryTrie();
    private final HashMap<String, Synset> synsets = new HashMap<>();
    private final HashMap<String, Sense> senses = new HashMap<>();

    public void addEntry(String id, LexicalEntry entry) {
        if(entriesById.put(id, entry) != null) {
            throw new IllegalArgumentException("Duplicate entry: " + id);
        }
        StringPair sp = new StringPair(entry.writtenFrom, entry.partOfSpeech);
        entriesByLemma.add(entry);
        for(Sense s : entry.senses) {
            senses.put(s.id, s);
        }
    }

    public void addSynset(String id, Synset synset) {
        if(synset == null)
            throw new IllegalArgumentException();
        if(synsets.put(id, synset) != null) {
            throw new IllegalArgumentException("Duplicate synset: " + id);
        }
    }

    public List<LexicalEntry> lookupEntry(String[] words, int i, int j, String partOfSpeech) {
        Map<String, List<LexicalEntry>> entries = entriesByLemma.find(words, i, j);
        return Collections.unmodifiableList(entries.getOrDefault(partOfSpeech, Collections.EMPTY_LIST));
    }

    public List<LexicalEntry> lookupEntry(String[] words, int i, int j) {
        Map<String, List<LexicalEntry>> entries = entriesByLemma.find(words, i, j);
        List<LexicalEntry> all = new ArrayList<>();
        for(List<LexicalEntry> l : entries.values()) {
            all.addAll(l);
        }
        return Collections.unmodifiableList(all);
    }

    public List<LexicalEntry> lookupEntry(String[] words) {
        return lookupEntry(words, 0, words.length);
    }

    public Map<String[], List<LexicalEntry>> allEntries(String[] words) {
        Map<String[], List<LexicalEntry>> le = new HashMap<>();
        for(int i = 0; i < words.length; i++) {
            le.putAll(entriesByLemma.findAll(words, i));
        }
        return le;
    }

    public Synset lookupSynset(String id) {
        if(!synsets.containsKey(id)) {
            throw new IllegalArgumentException("Missing identifier: " + id);
        }
        return synsets.get(id);
    }

    public Map<String, Set<Synset>> findRoots() {
        Map<String, Set<Synset>> roots = new HashMap<>();

        for(LexicalEntry le : entriesById.values()) {
            if(!roots.containsKey(le.partOfSpeech)) {
                roots.put(le.partOfSpeech, new HashSet<Synset>());
            }
            for(Sense s : le.senses) {
                Synset root = findRoot(lookupSynset(s.synset));
                roots.get(le.partOfSpeech).add(root);
            }
        }

        return roots;
    }

    public Synset findRoot(Synset s) {
        for(Synset.Relation r : s.relations) {
            if(r.relType.equals("hypernym") || r.relType.equals("instance_hypernym")) {
                return findRoot(lookupSynset(r.target));
            }
        }
        return s;
    }

    public Synset leastCommonSubsumer(Synset s1, Synset s2) {
        Synset s3 = s2;
        RIGHT: while(true) {
            if(s1.equals(s3)) {
                return s3;
            }
            for(Synset.Relation r : s3.relations) {
                if(r.relType.equals("hypernym") || r.relType.equals("instance_hypernym")) {
                    s3 = lookupSynset(r.target);
                    continue RIGHT;
                }
            }
            break;
        }
        for(Synset.Relation r : s1.relations) {
            if(r.relType.equals("hypernym") || r.relType.equals("instance_hypernym")) {
                return leastCommonSubsumer(lookupSynset(r.target), s2);
            }
        }
        return null;
    }

    public int depthTo(Synset from, Synset to) {
        if(from.equals(to)) {
            return 0;
        }
        for(Synset.Relation r : from.relations) {
            if(r.relType.equals("hypernym") || r.relType.equals("instance_hypernym")) {
                return 1 + depthTo(lookupSynset(r.target),to);
            }
        }
        throw new IllegalArgumentException("Not in hierachy");
    }

    public int depth(Synset s) {
        for(Synset.Relation r : s.relations) {
            if(r.relType.equals("hypernym") || r.relType.equals("instance_hypernym")) {
                return 1 + depth(lookupSynset(r.target));
            }
        }
        return 0;
    }

    public int maxDepth(Synset s) {
        int maxDepth = 0;
        for(Synset.Relation r : s.relations) {
            if(r.relType.equals("hyponym") || r.relType.equals("instance_hyponym")) {
                maxDepth = Math.max(maxDepth, 1 + maxDepth(lookupSynset(r.target)));
            }
        }
        return maxDepth;
    }

    public static class SynsetTokenization {
        public List<String> words;
        public List<List<Synset>> synsets;

        public SynsetTokenization() {
            words = new ArrayList<>();
            synsets = new ArrayList<>();
        }
    }

    public List<Synset> derivForms(Sense s) {
        List<Synset> ss = new ArrayList<>();
        for(Sense.Relation r : s.relations) {
            if(r.relType.equals("derivation")) {
                Sense target = senses.get(r.target);
                ss.add(lookupSynset(target.synset));
            }
        }
        return ss;
    }
        
    
    /*public SynsetTokenization identifySynsets(String[] tokens, Set<String> stopWords) {
        SynsetTokenization st = new SynsetTokenization();
        for(int i = 0; i < tokens.length; ) {
            int j = tokens.length;
            for(; j > i; j--) {
                String word = Strings.join(Arrays.copyOfRange(tokens, i, j), " ").toLowerCase();
                if(!stopWords.contains(word)) {
                    List<LexicalEntry> les = lookupEntry(word);
                    if(!les.isEmpty()) {
                        List<Synset> ss = new ArrayList<>();
                        for(LexicalEntry le : les) {
                            for(Sense s : le.senses) {
                                Synset s1 = lookupSynset(s.synset);
                                ss.add(s1);
                                ss.addAll(derivForms(s));
                            }
                        }
                        st.synsets.add(ss);
                        st.words.add(word);
                        break;
                    }
                }
            }
            i = Math.max(j,i+1);
        }
        return st;
    }*/
}
