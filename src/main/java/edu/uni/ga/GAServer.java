package edu.uni.ga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.uni.common.AppConfig;
import edu.uni.common.Json;
import edu.uni.common.Types;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.io.File;
import java.nio.file.Files;
import java.util.*;


public class GAServer {

    static class DB {
        public Map<String,Integer> librosDisponibles = new HashMap<>();
        public Map<String,Integer> renovaciones = new HashMap<>();
        public Set<String> aplicados = new HashSet<>();
    }

    private static final File DBFILE = new File("db.json");
    private static DB DBSTATE = new DB();

    public static void main(String[] args) throws Exception {
        String bindHost = args.length > 0 ? args[0] : "0.0.0.0";
        load();

        ZMQ.Context ctx = ZMQ.context(1);
        ZMQ.Socket rep = ctx.socket(SocketType.REP);
        rep.bind("tcp://" + bindHost + ":" + AppConfig.GA_REQREP_PORT);
        System.out.println("[GA] REP en tcp://" + bindHost + ":" + AppConfig.GA_REQREP_PORT);

        while (!Thread.currentThread().isInterrupted()) {
            byte[] reqBytes = rep.recv();
            Types.PeticionGA pet = Json.MAPPER.readValue(reqBytes, Types.PeticionGA.class);
            String key = pet.evt.idSolicitud + "|" + pet.op;

            Types.RespuestaGA resp;
            try {
                if (DBSTATE.aplicados.contains(key)) {
                    resp = Types.RespuestaGA.dup();
                } else {
                    switch (pet.op) {
                        case "devolucion" -> aplicarDevolucion(pet);
                        case "renovacion" -> aplicarRenovacion(pet);
                        default -> throw new IllegalArgumentException("Op no soportada: " + pet.op);
                    }
                    DBSTATE.aplicados.add(key);
                    save();
                    resp = Types.RespuestaGA.ok();
                }
            } catch (Exception ex) {
                resp = Types.RespuestaGA.error(ex.getMessage());
            }

            rep.send(Json.MAPPER.writeValueAsBytes(resp));
        }
        rep.close(); ctx.close();
    }

    private static void aplicarDevolucion(Types.PeticionGA pet) {
        String idLibro = String.valueOf(pet.evt.payload.get("idLibro"));
        int val = DBSTATE.librosDisponibles.getOrDefault(idLibro, 0) + 1;
        DBSTATE.librosDisponibles.put(idLibro, val);
        System.out.printf("[GA] DEV idLibro=%s disponibles=%d%n", idLibro, val);
    }

    private static void aplicarRenovacion(Types.PeticionGA pet) {
        String idPrestamo = String.valueOf(pet.evt.payload.get("idPrestamo"));
        int count = DBSTATE.renovaciones.getOrDefault(idPrestamo, 0);
        if (count >= 2) throw new IllegalStateException("Max 2 renovaciones");
        DBSTATE.renovaciones.put(idPrestamo, count + 1);
        System.out.printf("[GA] REN idPrestamo=%s renovaciones=%d%n", idPrestamo, count + 1);
    }

    private static void load() {
        try {
            if (DBFILE.exists()) {
                byte[] bytes = Files.readAllBytes(DBFILE.toPath());
                ObjectNode node = (ObjectNode) Json.MAPPER.readTree(bytes);
                DBSTATE.librosDisponibles = Json.MAPPER.convertValue(node.get("librosDisponibles"),
                        new TypeReference<Map<String,Integer>>() {});
                DBSTATE.renovaciones     = Json.MAPPER.convertValue(node.get("renovaciones"),
                        new TypeReference<Map<String,Integer>>() {});
                DBSTATE.aplicados        = new HashSet<>(Json.MAPPER.convertValue(node.get("aplicados"),
                        new TypeReference<List<String>>() {}));
                System.out.println("[GA] DB cargada (" + DBSTATE.aplicados.size() + " ops)");
            } else {
                save();
            }
        } catch (Exception e) {
            System.err.println("[GA] No se pudo cargar DB, iniciando nueva: " + e.getMessage());
        }
    }
    private static void save() {
        try {
            Map<String,Object> dump = new HashMap<>();
            dump.put("librosDisponibles", DBSTATE.librosDisponibles);
            dump.put("renovaciones", DBSTATE.renovaciones);
            dump.put("aplicados", DBSTATE.aplicados);
            Json.MAPPER.writerWithDefaultPrettyPrinter().writeValue(DBFILE, dump);
        } catch (Exception e) {
            System.err.println("[GA] Error guardando DB: " + e.getMessage());
        }
    }
}
