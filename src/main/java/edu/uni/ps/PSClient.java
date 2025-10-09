package edu.uni.ps;

import edu.uni.common.AppConfig;
import edu.uni.common.Json;
import edu.uni.common.Types;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalDate;
import java.util.*;

public class PSClient {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java PSClient <csvPath> <sede> <gcHost>");
            return;
        }
        String csv = args[0];
        String sede = args[1];
        String gcHost = args[2];

        ZMQ.Context ctx = ZMQ.context(1);
        ZMQ.Socket req = ctx.socket(SocketType.REQ);
        req.connect(AppConfig.gcReqRepEndpoint(gcHost));
        System.out.println("[PS] Conectado a GC: " + AppConfig.gcReqRepEndpoint(gcHost));

        try (BufferedReader br = new BufferedReader(new FileReader(csv))) {
            String header = br.readLine(); // consumir encabezado
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(";");
                String tipo = parts[0].trim();
                String idUsuario = parts.length > 1 ? parts[1].trim() : "";
                String idLibro   = parts.length > 2 ? parts[2].trim() : "";
                String idPrestamo= parts.length > 3 ? parts[3].trim() : "";

                Types.Evento evt = new Types.Evento();
                evt.idSolicitud = UUID.randomUUID().toString();
                evt.sede = sede;
                evt.timestamp = Types.now();
                evt.tipo = Types.TipoOperacion.valueOf(tipo); // DEVOLUCION o RENOVACION

                Map<String, Object> payload = new HashMap<>();
                if (!idUsuario.isEmpty()) payload.put("idUsuario", idUsuario);
                if (!idLibro.isEmpty())   payload.put("idLibro", idLibro);
                if (!idPrestamo.isEmpty())payload.put("idPrestamo", idPrestamo);
                evt.payload = payload;

                String json = Json.MAPPER.writeValueAsString(evt);
                req.send(json.getBytes(ZMQ.CHARSET));
                byte[] repBytes = req.recv();
                Types.RespuestaPS rep = Json.MAPPER.readValue(repBytes, Types.RespuestaPS.class);

                System.out.printf("[PS] %s → %s nuevaFecha=%s msg=%s%n",
                        evt.tipo, rep.status, rep.nuevaFecha, rep.mensaje);
                Thread.sleep(20); // think-time mínimo para demo
            }
        }
        req.close();
        ctx.close();
    }
}
