package edu.uni.tools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

public class SeedDB {
    static class DB {
        public Map<String,Integer> librosDisponibles = new HashMap<>();
        public Map<String,Integer> renovaciones = new HashMap<>();
        public Set<String> aplicados = new HashSet<>();
    }

    public static void main(String[] args) throws Exception {
        // Parámetros simples del catálogo
        int totalLibros = 1000;
        int ejemplaresPorLibro = 1;
        // IDs de libros L-0001 ... L-1000
        List<String> libros = new ArrayList<>();
        for (int i = 1; i <= totalLibros; i++) {
            libros.add(String.format("L-%04d", i));
        }

        DB base = new DB();
        for (String id : libros) {
            base.librosDisponibles.put(id, ejemplaresPorLibro);
        }

        // Construir Sede A (50 prestados) y Sede B (150 prestados)
        DB sedeA = clonar(base);
        DB sedeB = clonar(base);

        restarPrestados(sedeA.librosDisponibles, 50);
        restarPrestados(sedeB.librosDisponibles, 150);

        JsonFactory om = new ObjectMapper().writerWithDefaultPrettyPrinter().getFactory().setCodec(new ObjectMapper());
        ObjectMapper pretty = new ObjectMapper();

        pretty.writerWithDefaultPrettyPrinter().writeValue(new File("db_sedeA.json"), sedeA);
        pretty.writerWithDefaultPrettyPrinter().writeValue(new File("db_sedeB.json"), sedeB);

        System.out.println("Generados db_sedeA.json (50 prestados) y db_sedeB.json (150 prestados).");
        System.out.println("Catálogo idéntico; difiere la disponibilidad inicial por sede.");
    }

    private static DB clonar(DB src) {
        DB d = new DB();
        d.librosDisponibles.putAll(src.librosDisponibles);
        return d;
    }

    private static void restarPrestados(Map<String,Integer> disponibles, int cantidadPrestados) {
        int restados = 0;
        for (String id : disponibles.keySet()) {
            if (restados >= cantidadPrestados) break;
            int val = disponibles.getOrDefault(id, 0);
            if (val > 0) {
                disponibles.put(id, val - 1);
                restados++;
            }
        }
    }
}
