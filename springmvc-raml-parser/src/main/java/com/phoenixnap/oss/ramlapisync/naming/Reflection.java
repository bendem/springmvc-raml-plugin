package com.phoenixnap.oss.ramlapisync.naming;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author bendem (online@bendem.be)
 */
public class Reflection {

    public static class TypeResolutionException extends RuntimeException {
        public TypeResolutionException(String message) {
            super(message);
        }
    }

    public interface MaybeGenericClass<T> {
        Class<T> getMaybeGenericClass();
        boolean isGeneric();
    }
    public static class NonGenericClass<T> implements MaybeGenericClass<T> {
        private final Class<T> clazz;

        public NonGenericClass(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Class<T> getMaybeGenericClass() { return clazz; }

        @Override
        public boolean isGeneric() { return false; }

        @Override
        public String toString() {
            return clazz.getName();
        }
    }

    public static class GenericClass<T> implements MaybeGenericClass<T> {
        private final Class<T> clazz;
        private final List<MaybeGenericClass<?>> typeParameters;

        public GenericClass(Class<T> clazz, List<MaybeGenericClass<?>> typeParameters) {
            this.clazz = clazz;
            this.typeParameters = typeParameters;
        }

        @Override
        public Class<T> getMaybeGenericClass() { return clazz; }

        @Override
        public boolean isGeneric() { return true; }

        public List<MaybeGenericClass<?>> getTypeParameters() { return typeParameters; }

        @Override
        public String toString() {
            return clazz.getName()
                + "<"
                + typeParameters.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "))
                + ">";
        }
    }


    public static MaybeGenericClass<?> resolve(Class<?> holder, Type type) {
        if (type instanceof Class<?>) {
            return new NonGenericClass<>((Class<?>) type);
        }

        if (type instanceof ParameterizedType) {
            return resolve(holder, (ParameterizedType) type);
        }

        if (type instanceof TypeVariable<?>) {
            return resolve(holder, (TypeVariable<?>) type);
        }

        throw new TypeResolutionException("couldn't resolve type");
    }

    public static GenericClass<?> resolve(Class<?> holder, ParameterizedType type) {
        return new GenericClass<>(
            (Class<?>) type.getRawType(),
            Arrays.stream(type.getActualTypeArguments())
                .map(t -> {
                    if (t instanceof TypeVariable<?>) {
                        return resolve(holder, ((TypeVariable<?>) t));
                    }
                    return resolve(holder, t);
                })
                .collect(Collectors.toList()));
    }

    public static MaybeGenericClass<?> resolve(Class<?> holder, TypeVariable<?> typeVariable) {
        Type[] genericInterfaces = holder.getGenericInterfaces();
        Type genericSuperclass = holder.getGenericSuperclass();

        List<MaybeGenericClass<?>> classes = Stream.concat(Arrays.stream(genericInterfaces), Stream.of(genericSuperclass))
            .map(t -> resolve(holder, t, typeVariable))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        if (classes.size() != 1) {
            throw new TypeResolutionException("couldn't resolve actual type of " + typeVariable);
        }

        return classes.get(0);
    }

    private static MaybeGenericClass<?> resolve(Class<?> holder, Type type, TypeVariable<?> typeVariable) {
        if (!(type instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();
        int i = indexOfTypeVariable(rawType.getTypeParameters(), typeVariable);
        if (i == -1) {
            return null;
        }

        return resolve(holder, parameterizedType.getActualTypeArguments()[i]);
    }

    private static int indexOfTypeVariable(TypeVariable<?>[] typeParameters, TypeVariable<?> typeVariable) {
        for (int i = 0; i < typeParameters.length; i++) {
            if (typeParameters[i].equals(typeVariable)) {
                return i;
            }
        }
        return -1;
    }
}
