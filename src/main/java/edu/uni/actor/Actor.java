package edu.uni.actor;

import  edu.uni.common.AppConfig;
import edu.uni.common.Json;
import edu.uni.common.Types;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;

public class Actor {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Uso: java Actor <devolucion|renovacion> <gcHost> <gaHost>");
            return;
        }
        String topic = args[0].toLowerCase();
        String gcHost = args[1];
        String gaHost = args[2];

        ZMQ.Context ctx = ZMQ.context(1);

        ZMQ.Socket sub = ctx.socket(SocketType.SUB);
        sub.connect(AppConfig.gcPubEndpoint(gcHost));
        sub.setReceiveTimeOut(1000);
        sub.subscribe(topic.getBytes(ZMQ.CHARSET));
        System.out.println("[Actor] SUB topic='" + topic + "' en " + AppConfig.gcPubEndpoint(gcHost));

        ZMQ.Socket req = ctx.socket(SocketType.REQ);
        req.connect(AppConfig.gaEndpoint(gaHost));
        System.out.println("[Actor] REQ a GA en " + AppConfig.gaEndpoint(gaHost));

        while (!Thread.currentThread().isInterrupted()) {
            byte[] msg = sub.recv();
            if (msg == null) continue; // timeout -> seguir
            String frame = new String(msg, ZMQ.CHARSET);
            int idx = frame.indexOf(' ');
            String top = frame.substring(0, idx);
            String json = frame.substring(idx + 1);

            Types.Evento evt = Json.MAPPER.readValue(json, Types.Evento.class);
            Types.PeticionGA pet = new Types.PeticionGA();
            pet.op = top;
            pet.evt = evt;

            req.send(Json.MAPPER.writeValueAsBytes(pet));
            byte[] ackBytes = req.recv();
            Types.RespuestaGA ack = Json.MAPPER.readValue(ackBytes, Types.RespuestaGA.class);

            System.out.printf("[Actor] %s id=%s -> GA: %s%n", top, evt.idSolicitud, ack.status);
        }

        sub.close(); req.close(); ctx.close();
    }
}
