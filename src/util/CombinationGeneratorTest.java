package util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static util.CombinationGenerator.genAllVectorLE;
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

    @Test
    public void test2(){
        List<Integer> original = new ArrayList<>();
        for (int i = 1; i <= 5; i ++) {
            original.add(new Random().nextInt(10) + 1);
        }
        List<Vector<Integer>> l = genAllVectorLE(original);
        System.out.println(original);
        System.out.println(original.stream().reduce((x, y) -> x + y).get());
        System.out.println(l.size());
        DebugHelper.printList(l);
    }

}