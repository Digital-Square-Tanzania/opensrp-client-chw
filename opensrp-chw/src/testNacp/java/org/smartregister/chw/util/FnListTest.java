package org.smartregister.chw.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FnListTest {


        @Test
        public void testReduceSumOfIntegers() {
            FnList<Integer> list = FnList.from(new Integer[]{1, 2, 3, 4, 5});
            int sum = list.reduce(0,Integer::sum );
            Assert.assertEquals(15, sum);
        }

        @Test
        public void testReduceConcatenationOfStrings() {
            FnList<String> list = FnList.from(new String[]{"a", "b", "c"});
            String result = list.reduce("",(acc, item) -> acc + item);
            Assert.assertEquals("abc", result);
        }
        @Test
        public void testMapSquareIntegers() {
            FnList<Integer> list = FnList.from(new Integer[]{1, 2, 3, 4});
            FnList<Integer> squaredList = list.map(i -> i * i);

            List<Integer> expected = Arrays.asList(1, 4, 9, 16);
            Assert.assertEquals(expected, squaredList.list());
        }

        @Test
        public void testMapStringToLength() {
            FnList<String> list = FnList.from(new String[]{"apple", "banana", "cherry"});
            FnList<Integer> lengthList = list.map(String::length);
            List<Integer> expected = Arrays.asList(5, 6, 6);
            Assert.assertEquals(expected, lengthList.list());
        }

        @Test
        public void testUniqueHappyPath() {
            FnList<Integer> list = FnList.from(new Integer[] {1, 2, 2, 3, 4, 4, 4});
            FnList<Integer> uniqueList = list.unique();

            // Convert uniqueList to a regular list for easier assertion
            List<Integer> result = new ArrayList<>();
            uniqueList.forEach(result::add);

            Assert.assertEquals(Arrays.asList(1, 2, 3, 4), result);
        }

        @Test
        public void testUniqueEmptyList() {
            FnList<Integer> list = FnList.from(new Integer[] {});
            FnList<Integer> uniqueList = list.unique();

            Assert.assertFalse(uniqueList.iterator().hasNext());
        }

        @Test
        public void testUniqueNullValues() {
            FnList<Integer> list = FnList.from(new Integer[] {null, null, 3, null});
            FnList<Integer> uniqueList = list.unique();

            List<Integer> result = new ArrayList<>();
            uniqueList.forEach(result::add);

            Assert.assertEquals(Collections.singletonList(3), result);
        }

        @Test
        public void testUniqueAllDuplicates() {
            FnList<Integer> list = FnList.from(new Integer[] {5, 5, 5, 5});
            FnList<Integer> uniqueList = list.unique();

            List<Integer> result = new ArrayList<>();
            uniqueList.forEach(result::add);

            Assert.assertEquals(Collections.singletonList(5), result);
        }

        @Test
        public void mapShouldTransformList() {
            FnList<Integer> numbers = FnList.range(0, 5);
            List<String> result = numbers.map(i -> "Number: " + i).list();

            Assert.assertEquals(5, result.size());
            for (int i = 0; i < result.size(); i++) {
                Assert.assertEquals("Number: " + i, result.get(i));
            }
        }

        @Test
        public void mapShouldHandleEmptyList(){
            FnList<Integer> emptyList = FnList.from(new ArrayList<>());
            List<String> result = emptyList.map(i -> "Empty: " + i).list();

            Assert.assertTrue(result.isEmpty());
        }

        @Test
        public void mapShouldPreserveOrder() {
            FnList<Integer> numbers = FnList.range(0, 5);
            List<Integer> result = numbers.map(i -> i + 10).list();

            for (int i = 0; i < 5; i++) {
                Assert.assertEquals(Integer.valueOf(i + 10), result.get(i));
            }
        }

        @Test
        public void testReduceSum() {
            FnList<Integer> list = FnList.range(5); // 0, 1, 2, 3, 4
            int sum = list.reduce(0,Integer::sum);
            Assert.assertEquals(10, sum);
        }

        @Test
        public void testReduceMultiplication() {
            FnList<Integer> list = FnList.from(new Integer[] {1, 2, 3, 4});
            int result = list.reduce(1,(acc, item) -> acc * item);
            Assert.assertEquals(24, result);
        }

        @Test
        public void testReduceOnEmptyList() {
            FnList<Integer> list = FnList.from(new ArrayList<>());
            int result = list.reduce(0,Integer::sum);
            Assert.assertEquals(0, result);
        }

        @Test
        public void testReduceWithException() {
            FnList<Integer> list = FnList.range(5); // 0, 1, 2, 3, 4

            // This will cause a divide by zero exception
            int result = list.reduce(1,(acc, item) -> {
                if(item == 0) {
                    return acc / item;
                }
                return acc + item;
            });

            // Since exceptions are swallowed, we just verify the result
            // Note: This is more of a way to demonstrate that an exception was swallowed.
            // In real-life use-cases, you might want a more meaningful way of handling such exceptions.
            Assert.assertNotEquals(10, result); // This is just a way to demonstrate that an exception was swallowed
        }



}
