package a_lexico_robot;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

public class GeneradorLua {

    public static String generarCodigoLua(List<Integer> valores) {
        System.out.println("=== GeneradorLua: Secuencias DSL con 4 Motores ===");
        System.out.println("Valores recibidos: " + valores);

        if (valores.size() < 2) {
            System.out.println("ERROR: Se necesitan al menos 2 valores");
            return generarCodigoDefault();
        }

        // Analizar secuencia de movimientos desde los valores DSL
        List<MovimientoRobot> secuencia = analizarSecuenciaDSL(valores);
        
        if (secuencia.isEmpty()) {
            return generarCodigoDefault();
        }

        // Determinar valores finales para los 4 motores principales
        EstadoRobot estadoFinal = determinarEstadoFinal(secuencia);

        // Generar código Lua
        StringBuilder lua = new StringBuilder();

        // Encabezado y configuración inicial (mantener estructura original)
        lua.append("sim=require'sim'\n");
        lua.append("simIK=require'simIK'\n");
        lua.append("function sysCall_thread()\n");
        lua.append("    -- Get handles:\n");
        lua.append("    modelBase=sim.getObject('..')\n");
        lua.append("    gripperHandle=sim.getObject('../vacuumGripper')\n");
        lua.append("    pickupPart=sim.getObject('../pickupPart')\n");
        lua.append("    pickPos=sim.getObject('../pickPos')\n");
        lua.append("    tip=sim.getObject('../tip')\n");
        lua.append("    target=sim.getObject('../target')\n");
        lua.append("    sim.setObjectParent(pickupPart,-1,true)\n");
        lua.append("    local maxTorque=50\n\n");

        // Velocidades calculadas desde la secuencia DSL (4 motores + 2 auto-calculados)
        lua.append("    -- Movement params basados en secuencia DSL:\n");
        lua.append("    maxJointVel={\n");
        lua.append("        ").append(estadoFinal.velocidadBase).append("*math.pi/180,    -- joint1 (base): ").append(estadoFinal.velocidadBase).append("°/s\n");
        lua.append("        ").append(estadoFinal.velocidadHombro).append("*math.pi/180,   -- joint2 (hombro): ").append(estadoFinal.velocidadHombro).append("°/s\n");
        lua.append("        ").append(estadoFinal.velocidadCodo).append("*math.pi/180,     -- joint3 (codo): ").append(estadoFinal.velocidadCodo).append("°/s\n");
        lua.append("        ").append(estadoFinal.velocidadGarra).append("*math.pi/180,    -- joint4 (garra): ").append(estadoFinal.velocidadGarra).append("°/s\n");
        lua.append("        ").append((estadoFinal.velocidadHombro + estadoFinal.velocidadCodo) / 2).append("*math.pi/180,  -- joint5 (muñeca1): auto-calculada\n");
        lua.append("        30*math.pi/180   -- joint6 (muñeca2): velocidad estándar\n");
        lua.append("    }\n");

        lua.append("    maxJointAccel={40*math.pi/180,40*math.pi/180,40*math.pi/180,40*math.pi/180,40*math.pi/180,40*math.pi/180}\n");
        lua.append("    maxJointJerk={80*math.pi/180,80*math.pi/180,80*math.pi/180,80*math.pi/180,80*math.pi/180,80*math.pi/180}\n");

        // Parámetros IK (mantener original)
        lua.append("    -- Movement params for IK:\n");
        lua.append("    maxVel={0.1}\n");
        lua.append("    maxAccel={0.1}\n");
        lua.append("    maxJerk={0.5}\n");
        lua.append("    metric={1,1,1,0.1}\n");

        // Configuración IK (mantener original)
        lua.append("    -- Set-up IK:\n");
        lua.append("    ikEnv=simIK.createEnvironment()\n");
        lua.append("    ikGroup=simIK.createGroup(ikEnv)\n");
        lua.append("    ikElement,simToIkMap=simIK.addElementFromScene(ikEnv,ikGroup,modelBase,tip,target,simIK.constraint_pose)\n\n");

        // Configuración de joints (mantener original)
        lua.append("    -- Get more handles:\n");
        lua.append("    jointHandles={}\n");
        lua.append("    ikJointHandles={}\n");
        lua.append("    for i=1,6,1 do\n");
        lua.append("        jointHandles[i]=sim.getObject('../joint'..i)\n");
        lua.append("        ikJointHandles[i]=simToIkMap[jointHandles[i]]\n");
        lua.append("        sim.setJointTargetForce(jointHandles[i],maxTorque)\n");
        lua.append("        sim.setObjectFloatParam(jointHandles[i],sim.jointfloatparam_maxvel,maxJointVel[i])\n");
        lua.append("    end\n\n");

        lua.append("    local initConfig=getCurrentConfig()\n\n");

        // POSICIÓN HOME INICIAL
        lua.append("    -- POSICIÓN HOME INICIAL\n");
        lua.append("    print('🏠 Iniciando en posición HOME...')\n");
        lua.append("    local configHome = {\n");
        lua.append("        math.rad(0),      -- joint1 (base): 0°\n");
        lua.append("        math.rad(0),      -- joint2 (hombro): 0°\n");
        lua.append("        math.rad(0),      -- joint3 (codo): 0°\n");
        lua.append("        math.rad(0),      -- joint4 (garra): 0°\n");
        lua.append("        math.rad(0),      -- joint5 (muñeca1): 0°\n");
        lua.append("        math.rad(0)       -- joint6 (muñeca2): 0°\n");
        lua.append("    }\n");
        lua.append("    moveToConfig(configHome)\n");
        lua.append("    print('✅ Robot en posición HOME inicial')\n");
        lua.append("    sim.wait(2)\n\n");

        // EJECUTAR SECUENCIA DSL CON BUCLES
        lua.append("    -- SECUENCIA DSL CON POSIBLES BUCLES\n");
        lua.append("    print('🎯 Ejecutando secuencia DSL completa...')\n");
        
        // Configuración actual del robot
        lua.append("    local configActual = {0, 0, 0, 0, 0, 0} -- inicializar en HOME\n\n");
        
        int repeticionActual = 0;
        for (int i = 0; i < secuencia.size(); i++) {
            MovimientoRobot mov = secuencia.get(i);
            
            // Detectar nueva repetición
            if (mov.numeroRepeticion > 0 && mov.numeroRepeticion != repeticionActual) {
                repeticionActual = mov.numeroRepeticion;
                lua.append("    -- ===============================\n");
                lua.append("    -- REPETICIÓN ").append(repeticionActual).append("\n");
                lua.append("    -- ===============================\n");
                lua.append("    print('🔄 Iniciando repetición ").append(repeticionActual).append("...')\n");
            }
            
            lua.append("    -- Paso ").append(i + 1).append(": ").append(mov.articulacion).append(" = ").append(mov.grados).append("° (vel: ").append(mov.velocidad).append("°/s)");
            if (mov.numeroRepeticion > 0) {
                lua.append(" [Rep ").append(mov.numeroRepeticion).append("]");
            }
            lua.append("\n");
            
            lua.append("    print('").append(i + 1).append(". ").append(mov.articulacion.toUpperCase()).append(" → ").append(mov.grados).append("° (").append(mov.velocidad).append("°/s)");
            if (mov.numeroRepeticion > 0) {
                lua.append(" [Repetición ").append(mov.numeroRepeticion).append("]");
            }
            lua.append("')\n");
            
            // Actualizar solo la articulación específica
            int jointIndex = obtenerIndiceJoint(mov.articulacion);
            lua.append("    configActual[").append(jointIndex).append("] = math.rad(").append(mov.grados).append(")\n");
            
            // Auto-calcular muñeca1 basada en hombro + codo
            if (mov.articulacion.equals("hombro") || mov.articulacion.equals("codo")) {
                lua.append("    -- Auto-calcular muñeca1 para mantener orientación\n");
                lua.append("    configActual[5] = -(configActual[2] + configActual[3])\n");
            }
            
            // Actualizar velocidad específica para esta articulación
            lua.append("    maxJointVel[").append(jointIndex).append("] = ").append(mov.velocidad).append("*math.pi/180\n");
            lua.append("    sim.setObjectFloatParam(jointHandles[").append(jointIndex).append("],sim.jointfloatparam_maxvel,maxJointVel[").append(jointIndex).append("])\n");
            
            // Mover a la nueva configuración
            lua.append("    moveToConfig(configActual)\n");
            lua.append("    sim.wait(1.0)\n\n");
        }

        // CONFIGURACIÓN FINAL COMPLETA CON LOS 4 MOTORES DSL
        int muneca1Final = -(estadoFinal.hombro + estadoFinal.codo);
        lua.append("    -- CONFIGURACIÓN FINAL DSL (4 motores + 2 auto-calculados)\n");
        lua.append("    print('🤖 Aplicando configuración final completa...')\n");
        lua.append("    local configFinalDSL = {\n");
        lua.append("        math.rad(").append(estadoFinal.base).append("),     -- joint1 (base): ").append(estadoFinal.base).append("°\n");
        lua.append("        math.rad(").append(estadoFinal.hombro).append("),     -- joint2 (hombro): ").append(estadoFinal.hombro).append("°\n");
        lua.append("        math.rad(").append(estadoFinal.codo).append("),     -- joint3 (codo): ").append(estadoFinal.codo).append("°\n");
        lua.append("        math.rad(").append(estadoFinal.garra).append("),     -- joint4 (garra): ").append(estadoFinal.garra).append("°\n");
        lua.append("        math.rad(").append(muneca1Final).append("),     -- joint5 (muñeca1): ").append(muneca1Final).append("° [auto]\n");
        lua.append("        math.rad(0)      -- joint6 (muñeca2): 0° [neutral]\n");
        lua.append("    }\n");
        lua.append("    moveToConfig(configFinalDSL)\n");
        lua.append("    print('✅ Robot en configuración final DSL')\n");
        lua.append("    sim.wait(2)\n\n");

        // SECUENCIA ORIGINAL DE COPPELIASIM (mantener intacta)
        lua.append("    -- SECUENCIA ORIGINAL DE COPPELIASIM\n");
        lua.append("    print('🔄 Ejecutando secuencia original del robot...')\n");
        lua.append("    -- FK mov., find a config that corresponds to the desired pose, then move in FK:\n");
        lua.append("    local cubePickPose=sim.getObjectPose(pickPos,modelBase)\n");
        lua.append("    -- Search several possible configurations, and pick the one closest to current configuration:\n");
        lua.append("    simIK.setObjectPose(ikEnv, simToIkMap[target], cubePickPose, simToIkMap[modelBase])\n");
        lua.append("    local configs = simIK.findConfigs(ikEnv, ikGroup, ikJointHandles, {maxTime = 1.0, findMultiple = true, maxDist = 0.1, cMetric = {1.0, 1.0, 1.0, 1.0, 1.0, 0.1}})\n");
        lua.append("    if configs and #configs > 0 then\n");
        lua.append("        local config = configs[1]\n");
        lua.append("        moveToConfig(config)\n");
        lua.append("        print('📦 Robot posicionado para recoger objeto')\n\n");

        lua.append("        -- IK mov., describing a square:\n");
        lua.append("        cubePickPose[3]=cubePickPose[3]+0.04\n");
        lua.append("        setGripperOn(true)\n");
        lua.append("        print('🤏 Garra activada - objeto agarrado')\n");
        lua.append("        sim.wait(0.5)\n");
        lua.append("        moveToPose(cubePickPose)\n");
        lua.append("        cubePickPose[2]=-cubePickPose[2]\n");
        lua.append("        moveToPose(cubePickPose)\n");
        lua.append("        cubePickPose[3]=cubePickPose[3]+0.1\n");
        lua.append("        moveToPose(cubePickPose)\n");
        lua.append("        cubePickPose[2]=-cubePickPose[2]\n");
        lua.append("        moveToPose(cubePickPose)\n");
        lua.append("        cubePickPose[3]=cubePickPose[3]-0.14\n");
        lua.append("        moveToPose(cubePickPose)\n");
        lua.append("        setGripperOn(false)\n");
        lua.append("        print('✋ Objeto soltado en nueva ubicación')\n");
        lua.append("        sim.wait(0.5)\n");
        lua.append("    else\n");
        lua.append("        print('⚠️ No se encontró configuración IK válida')\n");
        lua.append("    end\n\n");

        // DECIDIR POSICIÓN FINAL
        boolean terminaEnHome = verificarSiTerminaEnHome(estadoFinal);
        
        if (terminaEnHome) {
            lua.append("    -- TERMINA EN HOME (detectado: todos los motores en 0°)\n");
            lua.append("    print('🏠 Regresando a posición HOME...')\n");
            lua.append("    moveToConfig(configHome)\n");
            lua.append("    print('✅ Robot en posición HOME final')\n");
        } else {
            lua.append("    -- MANTENER CONFIGURACIÓN DSL FINAL\n");
            lua.append("    print('🎯 Regresando a configuración DSL final...')\n");
            lua.append("    moveToConfig(configFinalDSL)\n");
            lua.append("    print('✅ Robot mantiene configuración DSL:')\n");
            lua.append("    print('   Base: ").append(estadoFinal.base).append("°, Hombro: ").append(estadoFinal.hombro).append("°, Codo: ").append(estadoFinal.codo).append("°, Garra: ").append(estadoFinal.garra).append("°')\n");
        }

        lua.append("    print('🎉 Secuencia completa terminada')\n");
        lua.append("end\n\n");

        // Funciones auxiliares (mantener originales)
        lua.append(generarFuncionesOriginales());

        System.out.println("✓ Código Lua generado para secuencia DSL");
        System.out.println("✓ Configuración final: Base=" + estadoFinal.base + "°, Hombro=" + estadoFinal.hombro + "°, Codo=" + estadoFinal.codo + "°, Garra=" + estadoFinal.garra + "°");
        System.out.println("✓ Termina en HOME: " + terminaEnHome);
        
        return lua.toString();
    }

    private static List<MovimientoRobot> analizarSecuenciaDSL(List<Integer> valores) {
        List<MovimientoRobot> secuencia = new ArrayList<>();
        
        System.out.println("🔍 Analizando secuencia DSL...");
        
        // Detectar si hay bucle repetir
        BucleInfo bucleInfo = detectarBucle(valores);
        
        if (bucleInfo.tieneBucle) {
            System.out.println("🔄 Bucle detectado: repetir " + bucleInfo.repeticiones + " veces");
            
            // Generar secuencia con repeticiones
            for (int rep = 0; rep < bucleInfo.repeticiones; rep++) {
                System.out.println("  --- Repetición " + (rep + 1) + " ---");
                
                List<MovimientoRobot> movimientosBucle = procesarMovimientosBucle(bucleInfo.valoresBucle, rep);
                secuencia.addAll(movimientosBucle);
            }
        } else {
            // Secuencia lineal sin bucles
            System.out.println("📝 Secuencia lineal detectada");
            secuencia.addAll(procesarSecuenciaLineal(valores));
        }
        
        return secuencia;
    }
    
    private static BucleInfo detectarBucle(List<Integer> valores) {
        BucleInfo info = new BucleInfo();
        
        // Detectar patrón de bucle basado en la cantidad de valores
        // Si hay exactamente 16 valores: 2 repeticiones de 8 movimientos (4 articulaciones x 2)
        if (valores.size() == 16) {
            // Verificar si los primeros 8 se repiten en los siguientes 8
            boolean esBucle = true;
            for (int i = 0; i < 8 && esBucle; i++) {
                if (!valores.get(i).equals(valores.get(i + 8))) {
                    esBucle = false;
                }
            }
            
            if (esBucle) {
                info.tieneBucle = true;
                info.repeticiones = 2;
                info.valoresBucle = valores.subList(0, 8);
                System.out.println("✅ Bucle automático detectado: 2 repeticiones de 8 valores");
            }
        }
        
        // Si no se detecta bucle automático, buscar patrón de retorno a 0
        if (!info.tieneBucle && valores.size() >= 16) {
            // Buscar secuencia que termine en todos 0s
            List<Integer> ultimosOcho = valores.subList(valores.size() - 8, valores.size());
            if (esSecuenciaHome(ultimosOcho)) {
                info.tieneBucle = true;
                info.repeticiones = 1;
                info.valoresBucle = valores.subList(0, valores.size() - 8);
                System.out.println("✅ Secuencia con retorno a HOME detectada");
            }
        }
        
        if (!info.tieneBucle) {
            info.repeticiones = 1;
            info.valoresBucle = valores;
        }
        
        return info;
    }
    
    private static boolean esSecuenciaHome(List<Integer> valores) {
        // Verificar si los valores de articulaciones son 0 (posiciones pares)
        for (int i = 0; i < valores.size(); i += 2) {
            if (valores.get(i) != 0) {
                return false;
            }
        }
        return true;
    }
    
    private static List<MovimientoRobot> procesarMovimientosBucle(List<Integer> valores, int numeroRepeticion) {
        List<MovimientoRobot> movimientos = new ArrayList<>();
        String[] patronArticulaciones = {"garra", "base", "hombro", "codo"};
        
        for (int i = 0; i < valores.size() - 1; i += 2) {
            int valorArticulacion = valores.get(i);
            int velocidad = valores.get(i + 1);
            
            String articulacion = patronArticulaciones[(i / 2) % patronArticulaciones.length];
            
            MovimientoRobot mov = new MovimientoRobot(articulacion, valorArticulacion, velocidad);
            mov.numeroRepeticion = numeroRepeticion + 1;
            movimientos.add(mov);
            
            System.out.println("    " + (i/2 + 1) + ". " + articulacion.toUpperCase() + ": " + valorArticulacion + "° (vel: " + velocidad + "°/s)");
        }
        
        return movimientos;
    }
    
    private static List<MovimientoRobot> procesarSecuenciaLineal(List<Integer> valores) {
        List<MovimientoRobot> movimientos = new ArrayList<>();
        String[] patronArticulaciones = {"garra", "base", "hombro", "codo"};
        
        for (int i = 0; i < valores.size() - 1; i += 2) {
            int valorArticulacion = valores.get(i);
            int velocidad = valores.get(i + 1);
            
            String articulacion = patronArticulaciones[(i / 2) % patronArticulaciones.length];
            
            MovimientoRobot mov = new MovimientoRobot(articulacion, valorArticulacion, velocidad);
            movimientos.add(mov);
            
            System.out.println("  " + (i/2 + 1) + ". " + articulacion.toUpperCase() + ": " + valorArticulacion + "° (vel: " + velocidad + "°/s)");
        }
        
        return movimientos;
    }

    private static EstadoRobot determinarEstadoFinal(List<MovimientoRobot> secuencia) {
        EstadoRobot estado = new EstadoRobot();
        
        // Encontrar los últimos valores de cada articulación
        for (MovimientoRobot mov : secuencia) {
            switch (mov.articulacion.toLowerCase()) {
                case "base" -> {
                    estado.base = mov.grados;
                    estado.velocidadBase = mov.velocidad;
                }
                case "hombro" -> {
                    estado.hombro = mov.grados;
                    estado.velocidadHombro = mov.velocidad;
                }
                case "codo" -> {
                    estado.codo = mov.grados;
                    estado.velocidadCodo = mov.velocidad;
                }
                case "garra" -> {
                    estado.garra = mov.grados;
                    estado.velocidadGarra = mov.velocidad;
                }
            }
        }
        
        return estado;
    }

    private static int obtenerIndiceJoint(String articulacion) {
        return switch (articulacion.toLowerCase()) {
            case "base" -> 1;
            case "hombro" -> 2;
            case "codo" -> 3;
            case "garra" -> 4;
            default -> 1;
        };
    }

    private static boolean verificarSiTerminaEnHome(EstadoRobot estado) {
        return estado.base == 0 && estado.hombro == 0 && estado.codo == 0 && estado.garra == 0;
    }

    private static String generarFuncionesOriginales() {
        return """
                -- FUNCIONES AUXILIARES ORIGINALES
                function setGripperOn(isOn)
                    if isOn then
                        sim.writeCustomStringData(gripperHandle,'gripperOn','on')
                    else
                        sim.writeCustomStringData(gripperHandle,'gripperOn','')
                    end
                end

                function getCurrentConfig()
                    local conf={}
                    for i=1,#jointHandles,1 do
                        conf[i]=sim.getJointPosition(jointHandles[i])
                    end
                    return conf
                end

                function moveToConfig(targetConf)
                    local params = {
                        joints = jointHandles,
                        targetPos = targetConf,
                        maxVel = maxJointVel,
                        maxAccel = maxJointAccel,
                        maxJerk = maxJointJerk,
                    }
                    sim.moveToConfig(params)
                end

                function ikMovCallback(data)
                    sim.setObjectPose(target, data.pose, modelBase)
                    simIK.handleGroup(ikEnv, ikGroup, {syncWorlds = true, allowError = true})
                end

                function moveToPose(targetPose)
                    local currentPose=sim.getObjectPose(tip,modelBase)
                    sim.setObjectPose(target,currentPose,modelBase)
                    local params = {
                        pose = currentPose,
                        targetPose = targetPose,
                        maxVel = maxVel,
                        maxAccel = maxAccel,
                        maxJerk = maxJerk,
                        callback = ikMovCallback,
                        metric = metric,
                    }
                    sim.moveToPose(params)
                end
                """;
    }

    private static String generarCodigoDefault() {
        StringBuilder lua = new StringBuilder();
        lua.append("sim=require'sim'\n");
        lua.append("function sysCall_thread()\n");
        lua.append("    print('❌ Error: Valores DSL insuficientes')\n");
        lua.append("    print('🔧 Se requieren pares: articulacion, velocidad')\n");
        lua.append("    print('📝 Orden: garra, vel, base, vel, hombro, vel, codo, vel')\n");
        lua.append("end\n");
        return lua.toString();
    }

    // Clases auxiliares
    private static class MovimientoRobot {
        String articulacion;
        int grados;
        int velocidad;
        int numeroRepeticion = 0; // 0 = sin repetición, >0 = número de repetición
        
        MovimientoRobot(String articulacion, int grados, int velocidad) {
            this.articulacion = articulacion;
            this.grados = grados;
            this.velocidad = velocidad;
        }
    }

    private static class EstadoRobot {
        int base = 0, hombro = 0, codo = 0, garra = 0;
        int velocidadBase = 30, velocidadHombro = 30, velocidadCodo = 30, velocidadGarra = 30;
    }
    
    private static class BucleInfo {
        boolean tieneBucle = false;
        int repeticiones = 1;
        List<Integer> valoresBucle = new ArrayList<>();
    }

    // Método para guardar código
    public static void guardarCodigoLua(String directorio, String nombreArchivo, String contenidoLua) {
        try {
            java.io.File dir = new java.io.File(directorio);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String rutaCompleta = directorio + "\\" + nombreArchivo + ".lua";

            try (FileWriter writer = new FileWriter(rutaCompleta)) {
                writer.write(contenidoLua);
                System.out.println("✓ Código Lua guardado: " + rutaCompleta);
            }
        } catch (IOException e) {
            System.err.println("✗ Error al guardar: " + e.getMessage());
        }
    }
}