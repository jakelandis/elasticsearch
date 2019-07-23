package org.elasticsearch.http.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ObjectModel {

    final String name;
    final String parent;
    public ObjectModel(String name, String parent) {
        this.name = name;
        this.parent = parent;
    }
    List<Field> fields = new ArrayList<>();

    static class Field {
        final String name;

        public Field(String name, String type) {
            this.name = name;
            this.type = type;
        }

        final String type;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Field field = (Field) o;
            return Objects.equals(name, field.name) &&
                Objects.equals(type, field.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }

        @Override
        public String toString() {
            return "Field{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
        }
    }


}
