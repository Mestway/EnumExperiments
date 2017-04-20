package main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import forward_enumeration.enum_abstract.AbsQueryEnumerator;

import static main.Synthesizer.Synthesize;
import global.GlobalConfig;

import java.util.List;

public class Main {

    @Parameter( arity = 1, description = "Target File", required = true)
    private List<String> targetFile;

    @Parameter(names={"--pruneWithConstraint", "-c"})
    private boolean pruneWithConstraint;

    @Parameter(names={"--pruneWithApproximation", "-a"})
    private boolean pruneWithApproximation = true;

    @Parameter(names={"--noPruning"})
    private boolean noPruning = false;

    @Parameter(names={"--complex-aggr-depth"})
    private int testComplexAggr = -1;

    @Parameter(names={"--depth", "-d"})
    private int searchDepth = -1;

    public static void main(String[] args) {

        Main main = new Main();
        JCommander commander = new JCommander();
        commander.addObject(main);
        commander.parse(args);

        main.run();
    }

    public void run() {

        if (noPruning)
            GlobalConfig.firstPhasePruning = GlobalConfig.PruningApproach.nothing;
        else if (pruneWithConstraint)
            GlobalConfig.firstPhasePruning = GlobalConfig.PruningApproach.constraint;
        else
            GlobalConfig.firstPhasePruning = GlobalConfig.PruningApproach.approximation;

        if (searchDepth > 0)
            GlobalConfig.maxSearchDepth = searchDepth;

        if (testComplexAggr > 0)
            GlobalConfig.testComplexAggregation = testComplexAggr;

        System.out.println("[Start Testing] " + targetFile + " "
                + GlobalConfig.firstPhasePruning
                + " depth:" + GlobalConfig.maxSearchDepth);
        Synthesize(targetFile.get(0), new AbsQueryEnumerator());
    }
}
