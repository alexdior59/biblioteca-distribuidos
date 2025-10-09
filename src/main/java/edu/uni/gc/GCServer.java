package edu.uni.gc;

import edu.uni.common.AppConfig;
import edu.uni.common.Json;
import edu.uni.common.Types;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

import java.time.LocalDate;

public class GCServer {
    public static void main(String[] args) throws Exception {
        String bindHost = args.length > 0 ? args[0] : "0.0.0.0";

        ZMQ.Context ctx = ZMQ.context(1);
        ZMQ.Socket rep = ctx.socket(SocketType.REP);
        rep.bind("tcp://" + bindHost + ":" + AppConfig.GC_REQREP_PORT);

        ZMQ.Socket pub = ctx.socket(SocketType.PUB);
        pub.bind("tcp://" + bindHost + ":" + AppConfig.GC_PUBSUB_PORT);

        System.out.println("[GC] REP en " + "tcp://" + bindHost + ":" + AppConfig.GC_REQREP_PORT);
        System.out.println("[GC] PUB en " + "tcp://" + bindHost + ":" + AppConfig.GC_PUBSUB_PORT);

        while (!Thread.currentThread().isInterrupted()) {
            byte[] reqBytes = rep.recv();
            Types.Evento evt = Json.MAPPER.readValue(reqBytes, Types.Evento.class);

            Types.RespuestaPS respuesta;
            switch (evt.tipo) {
                case RENOVACION -> {
                    String nueva = LocalDate.now().plusDays(7).toString();
                    respuesta = Types.RespuestaPS.okRenovacion(nueva);
                    rep.send(Json.MAPPER.writeValueAsBytes(respuesta));
                    publish(pub, "renovacion", evt);
                }
                case DEVOLUCION -> {
                    respuesta = Types.RespuestaPS.ok();
                    rep.send(Json.MAPPER.writeValueAsBytes(respuesta));
                    publish(pub, "devolucion", evt);
                }
                default -> {
                    respuesta = Types.RespuestaPS.unsupported();
                    rep.send(Json.MAPPER.writeValueAsBytes(respuesta));
                }
            }
        }
        rep.close(); pub.close(); ctx.close();
    }

    private static void publish(ZMQ.Socket pub, String topic, Types.Evento evt) throws Exception {
        String payload = Json.MAPPER.writeValueAsString(evt);
        String frame = topic + " " + payload;
        pub.send(frame.getBytes(ZMQ.CHARSET));
        System.out.printf("[GC] PUB %s id=%s sede=%s%n", topic, evt.idSolicitud, evt.sede);
    }
}
