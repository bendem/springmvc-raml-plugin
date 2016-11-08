package com.phoenixnap.oss.ramlapisync.naming;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class ReflectionTest {

    public static class Something<T> {
        public ResponseEntity<T> something(T t) {
            return null;
        }
    }

    public static class SomethingMore extends Something<Integer> {
        @Override
        public ResponseEntity<Integer> something(Integer integer) {
            return null;
        }
    }

    @Test
    public void test() throws Exception {
        Reflection.MaybeGenericClass<?> resolved = Reflection.resolve(SomethingMore.class, SomethingMore.class.getMethods()[0].getGenericReturnType());
        Assert.assertTrue(resolved.isGeneric());
        Assert.assertEquals(ResponseEntity.class, resolved.getMaybeGenericClass());
        Assert.assertEquals(Integer.class, ((Reflection.GenericClass<?>) resolved).getTypeParameters().get(0).getMaybeGenericClass());
    }
}
