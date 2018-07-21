package edu.stanford.nlp.pipeline;

import junit.framework.TestCase;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.util.*;

public class NERBenchmarkITest extends TestCase {

  String NER_BENCHMARK_WORKING_DIR = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir";

  private static final Pattern FB1_Pattern = Pattern.compile("FB1:  (\\d+\\.\\d+)");

  public List<Pair<String, List<String>>> loadCoNLLDocs(String filePath) {
    List<Pair<String, List<String>>> returnList = new ArrayList<Pair<String, List<String>>>();
    String currDoc = "";
    List<String> currNERTagList = new ArrayList<String>();
    List<String> conllLines = IOUtils.linesFromFile(filePath);
    conllLines.add("");
    for (String conllLine : conllLines) {
      if (conllLine.equals("")) {
        // remove the extra " "
        if (currDoc.length() > 0) {
          currDoc = currDoc.substring(0, currDoc.length() - 1);
          Pair<String, List<String>> docPair = new Pair<>(currDoc, currNERTagList);
          returnList.add(docPair);
        }
        currDoc = "";
        currNERTagList = new ArrayList<>();
      } else {
        currDoc += (conllLine.split("\t")[0] + " ");
        currNERTagList.add(conllLine.split("\t")[1]);
      }
    }
    return returnList;
  }

  public List<Annotation> createPipelineAnnotations(List<Pair<String, List<String>>> conllDocs,
                                                    StanfordCoreNLP pipeline) {
    List<Annotation> returnList = new ArrayList<Annotation>();

    for (Pair<String, List<String>> conllDoc : conllDocs) {
      Annotation conllDocAnnotation = new Annotation(conllDoc.first());
      pipeline.annotate(conllDocAnnotation);
      returnList.add(conllDocAnnotation);
    }

    return returnList;
  }

  public void writePerlScriptInputToPath(List<Annotation> annotations,
                                         List<Pair<String, List<String>>> conllDocs,
                                         String filePath) throws IOException {
    String perlScriptInput = "";
    for (int docNum = 0 ; docNum < annotations.size() ; docNum++) {
      Annotation currAnnotation = annotations.get(docNum);
      Pair<String, List<String>> currCoNLLDoc = conllDocs.get(docNum);
      List<CoreLabel> currAnnotationTokens = currAnnotation.get(CoreAnnotations.TokensAnnotation.class);
      for (int tokenNum = 0 ;
           tokenNum < currAnnotationTokens.size() ; tokenNum++) {
        String perlScriptLine = currAnnotationTokens.get(tokenNum).word()
            + "\t" + currCoNLLDoc.second().get(tokenNum) + "\t" + currAnnotationTokens.get(tokenNum).ner();
        perlScriptInput += (perlScriptLine + "\n");
      }
      perlScriptInput += "\n";
    }
    // remove last newline
    perlScriptInput = perlScriptInput.substring(0, perlScriptInput.length()-1);
    IOUtils.writeStringToFile(perlScriptInput, filePath, "UTF-8");
  }

  public String runEvalScript(String inputCoNLLFile) throws IOException{
    String result = null;
    String evalCmd = NER_BENCHMARK_WORKING_DIR+"/eval_conll_cmd.sh "+inputCoNLLFile;
    Process p = Runtime.getRuntime().exec(evalCmd);
    BufferedReader in =
        new BufferedReader(new InputStreamReader(p.getInputStream()));
    String inputLine;
    while ((inputLine = in.readLine()) != null) {
      System.out.println(inputLine);
      result += inputLine + "\n";
    }
    in.close();
    return result;
  }

  public double parseResults(String conllEvalScriptResults) {
    String[] resultLines = conllEvalScriptResults.split("\n");
    double foundF1Score = 0.0;
    for (String resultLine : resultLines) {
      Matcher m = FB1_Pattern.matcher(resultLine);
      // Should parse the F1 after "FB1:"
      if (m.find()) {
        String f1 = m.group(1);
        foundF1Score = Double.parseDouble(f1);
        break;
      }
    }
    return foundF1Score;
  }

  public void testEnglishNEROnCoNLLDev() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/conll.4class.testa";
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP englishPipeline = new StanfordCoreNLP(props);
    runNERTest("CoNLL 2003 English Dev", englishPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        94.01);
  }

  public void testEnglishNEROnCoNLLTest() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/conll.4class.testb";
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.conll.4class.distsim.crf.ser.gz");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP englishPipeline = new StanfordCoreNLP(props);
    runNERTest("CoNLL 2003 English Test", englishPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        90.24);
  }

  public void testEnglishNEROnOntoNotesDev() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/ontonotes.3class.dev";
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP englishPipeline = new StanfordCoreNLP(props);
    runNERTest("OntoNotes English Dev 3 Class", englishPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        89.93);
  }

  public void testEnglishNEROnOntoNotesTest() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/ontonotes.3class.test";
    Properties props = new Properties();
    props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP englishPipeline = new StanfordCoreNLP(props);
    runNERTest("OntoNotes English Test 3 Class", englishPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        90.77);
  }

  public void testGermanNEROnOntoNotesDev() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/deu.io.f15.utf8.testa";
    Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties");
    props.setProperty("annotators", "tokenize,ssplit,pos,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP germanPipeline = new StanfordCoreNLP(props);
    runNERTest("German CoNLL Dev 4 Class ", germanPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        99.0);
  }

  public void testGermanNEROnOntoNotesTest() throws IOException {
    String conllTestPath = "/u/scr/nlp/data/stanford-corenlp-testing/ner-benchmark-working-dir/deu.io.f15.utf8.testb";
    Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties");
    props.setProperty("annotators", "tokenize,ssplit,pos,ner");
    props.setProperty("tokenize.whitespace", "true");
    props.setProperty("ner.applyFineGrained", "false");
    props.setProperty("ner.useSUTime", "false");
    props.setProperty("ner.applyNumericClassifiers", "false");
    StanfordCoreNLP germanPipeline = new StanfordCoreNLP(props);
    runNERTest("German CoNLL Test 4 Class ", germanPipeline, NER_BENCHMARK_WORKING_DIR, conllTestPath,
        99.0);
  }


  public void runNERTest(String testName, StanfordCoreNLP pipeline, String workingDir, String goldFilePath,
                         double f1Threshold) throws IOException {
    // load gold data
    List<Pair<String, List<String>>> conllDocs = loadCoNLLDocs(goldFilePath);
    List<Annotation> conllAnnotations = createPipelineAnnotations(conllDocs, pipeline);
    // annotate and prepare perl eval script input data
    writePerlScriptInputToPath(conllAnnotations, conllDocs, workingDir+"/conllEvalInput.txt");
    System.err.println("---");
    System.err.println("running perl eval script for "+testName);
    // get results
    String conllEvalScriptResults = runEvalScript(workingDir+"/conllEvalInput.txt");
    double modelScore = parseResults(conllEvalScriptResults);
    assertTrue(String.format(testName+" failed: should have found F1 of at least %.2f but found F1 of %.2f",
        f1Threshold, modelScore), (modelScore >= f1Threshold));
    System.err.println("Current F1 score for "+testName+" is: "+modelScore);
  }

}