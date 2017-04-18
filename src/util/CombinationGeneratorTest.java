package util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static util.CombinationGenerator.genCombination;

/**
 * Created by clwang on 4/4/17.
 */
public class CombinationGeneratorTest {

    @Test
    public void test(){
        List<Integer> original = new ArrayList<>();
        for (int i = 1; i <= 10; i ++) {
            original.add(i);
        }
        List<List<Integer>> l = genCombination(original);
        DebugHelper.printList(l);
    }

}