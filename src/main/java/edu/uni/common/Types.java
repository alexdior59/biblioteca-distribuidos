package edu.uni.common;

import java.time.Instant;
import java.util.Map;

public final class Types {

    public enum TipoOperacion { DEVOLUCION, RENOVACION /* (PRESTAMO en entrega 2) */ }

    public static class Evento {
        public String idSolicitud;
        public TipoOperacion tipo;
        public String sede;
        public long timestamp;
        public Map<String, Object> payload;
    }

    // Respuesta GC->PS
    public static class RespuestaPS {
        public String status;
        public String nuevaFecha;
        public String mensaje;
        public static RespuestaPS ok() { var r = new RespuestaPS(); r.status = "OK"; return r; }
        public static RespuestaPS okRenovacion(String fecha) { var r = ok(); r.nuevaFecha = fecha; return r; }
        public static RespuestaPS unsupported() { var r = new RespuestaPS(); r.status = "UNSUPPORTED"; return r; }
        public static RespuestaPS error(String msg) { var r = new RespuestaPS(); r.status = "ERROR"; r.mensaje = msg; return r; }
    }

    // Mensaje Actor->GA
    public static class PeticionGA {
        public String op;
        public Evento evt;
    }

    // Respuesta GA->Actor
    public static class RespuestaGA {
        public String status;
        public String mensaje;
        public static RespuestaGA ok() { var r = new RespuestaGA(); r.status = "OK"; return r; }
        public static RespuestaGA dup() { var r = new RespuestaGA(); r.status = "DUP"; return r; }
        public static RespuestaGA error(String msg) { var r = new RespuestaGA(); r.status = "ERROR"; r.mensaje = msg; return r; }
    }

    public static long now() { return Instant.now().toEpochMilli(); }

    private Types() {}
}
