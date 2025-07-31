package com.ufersa.tcc.sistmonitoramento.server.firewall.NLP;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.List;

public class Analyzer {

    StanfordCoreNLP stanfordCoreNLP = Pipeline.getPipeline();

    public void run() {

        String text = "You said now you just love me as your wife. It let me so sad. I would go to Brazilia if I'm not poor.";
        CoreDocument cd = new CoreDocument(text);
        stanfordCoreNLP.annotate(cd);
        List<CoreLabel> cl = cd.tokens();
        List<CoreSentence> st = cd.sentences();
        for (CoreLabel label : cl) {
            System.out.print(label.lemma() + " ");
            String ner = label.get(CoreAnnotations.NamedEntityTagAnnotation.class);
            System.out.println(label.originalText() + " : " + ner);
        }
        System.out.println();
        for (CoreSentence sentence : st) {
            System.out.println(sentence.toString() + " -> Sentiment: " + sentence.sentiment());
        }

    }

    public void analyze(String text) {
        CoreDocument coreDocument = new CoreDocument(text);
        stanfordCoreNLP.annotate(coreDocument);
        List<CoreLabel> coreLabels = coreDocument.tokens();
        List<CoreSentence> coreSentences = coreDocument.sentences();
        for (CoreLabel label : coreLabels) {
            System.out.println(label.originalText() + ": " + label.lemma());
        }
        System.out.println();
        for (CoreSentence sentence : coreSentences) {
            System.out.println(sentence.toString() + " -> Sentiment: " + sentence.sentiment());
        }

        coreDocument.tokens();
    }


    public static void main(String[] args) {
        Analyzer analyzer = new Analyzer();

        analyzer.analyze("I want the logs from 04/18/2025 to 07/18/2025");
    }

}
