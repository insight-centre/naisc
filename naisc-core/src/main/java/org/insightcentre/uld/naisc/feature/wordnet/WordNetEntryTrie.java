package org.insightcentre.uld.naisc.feature.wordnet;

import org.insightcentre.uld.naisc.util.PrettyGoodTokenizer;

import java.util.*;

public class WordNetEntryTrie {
    private final WNETEntry root = new WNETEntry();

    private static class WNETEntry {
        Map<String, WNETEntry> next = new HashMap<>();
        Map<String, List<LexicalEntry>> entries = new HashMap<>();
    }

    public void add(LexicalEntry le) {
        String[] tokens = PrettyGoodTokenizer.tokenize(le.writtenFrom);
        WNETEntry node = this.root;
        for(int i = 0; i < tokens.length; i++) {
            final WNETEntry entry;
            if(!root.next.containsKey(tokens[i])) {
                entry = new WNETEntry();
                root.next.put(tokens[i], entry);
            } else {
                entry = root.next.get(tokens[i]);
            }
            if(i + 1 == tokens.length) {
                if(!entry.entries.containsKey(le.partOfSpeech)) {
                    entry.entries.put(le.partOfSpeech, new ArrayList<>());
                }
                entry.entries.get(le.partOfSpeech).add(le);
            }
            node = entry;
        }
    }

    public Map<String, List<LexicalEntry>> find(String[] tokens, int i, int j) {
        WNETEntry node = this.root;
        for(; i < j; i++) {
            if(node.next.containsKey(tokens[i])) {
                node = node.next.get(tokens[i]);
            } else {
                return Collections.EMPTY_MAP;
            }
        }
        return node.entries;
    }

    public Map<String[], List<LexicalEntry>> findAll(String[] tokens, int i0) {
        WNETEntry node = this.root;
        Map<String[], List<LexicalEntry>> all = new HashMap<>();
        for(int i = i0; i < tokens.length; i++) {
            List<LexicalEntry> l2 = new ArrayList<>();
            for(List<LexicalEntry> l : node.entries.values()) {
                l2.addAll(l);
            }
            all.put(Arrays.copyOfRange(tokens, i0, i + 1), l2);
            if(node.next.containsKey(tokens[i])) {
                node = node.next.get(tokens[i]);
            } else {
                return all;
            }
        }
        List<LexicalEntry> l2 = new ArrayList<>();
        for(List<LexicalEntry> l : node.entries.values()) {
            l2.addAll(l);
        }
        return all;
    }

}
