package main;

import lang.table.Table;
import lang.sql.ast.abstable.AbsTableNode;
import lang.sql.datatype.NumberVal;
import forward_enumeration.AbstractTableEnumerator;
import forward_enumeration.context.EnumConfig;
import global.GlobalConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by clwang on 3/22/16.
 */
public class Synthesizer {

    public static long TimeOut = 600000;

    public static List<AbsTableNode> Synthesize(String exampleFilePath, AbstractTableEnumerator enumerator) {

        // read file
        ExampleDS exampleDS = ExampleDS.readFromFile(exampleFilePath);

        if (GlobalConfig.PRINT_LOG) {
            System.out.println("[[Synthesis start]]");
            System.out.println("\tFile: " + exampleFilePath);
            System.out.println("\tEnumerator: " + enumerator.getClass().getSimpleName());
        }

        long timeUsed = 0;
        long timeStart = System.currentTimeMillis();

        EnumConfig config = exampleDS.enumConfig;
        List<Table> inputs = exampleDS.inputs;
        Table output = exampleDS.output;

        List<AbsTableNode> candidates = new ArrayList<>();

        if (GlobalConfig.GUESS_ADDITIONAL_CONSTANTS) {
            // guess constants
            Set<NumberVal> guessedNumConstants = SynthesizerHelper.guessExtraConstants(config.getAggrFuns(), inputs);
            config.addConstVals(guessedNumConstants.stream().collect(Collectors.toSet()));
        }

        int depth = GlobalConfig.maxSearchDepth;

        if (GlobalConfig.PRINT_LOG)
            System.out.println("[[Synthesis Depth]] " + depth);

        //##### Synthesis
        config.setMaxDepth(depth);
        candidates.addAll(enumerator.enumProgramWithIO(inputs, output, config));

        System.out.println("[[Candidates]] " + candidates.size());

        // formatting time
        timeUsed = System.currentTimeMillis() - timeStart;
        long second = (timeUsed / 1000) % 60;
        long minute = (timeUsed / (1000 * 60)) % 60;

        if (GlobalConfig.PRINT_LOG) {
            //System.out.println("[[Synthesis Status]] " + (candidates.isEmpty() ? "Failed" : "Succeeded"));
            System.out.printf("[[Synthesis Time]] %.3fs\n", (minute * 60. + second + 0.001 * (timeUsed % 1000)));
        }
        return candidates;
    }

    /**
     * Check whether any one of the candidate is a potentially correct candidate based on its filter score
     * @param candidates the set of candidate queries to be checked
     * @return whether a desirable one is contained.
     */
    public static boolean containsDesirableCandidate(List<AbsTableNode> candidates) {
        return (! candidates.isEmpty());
    }

}
