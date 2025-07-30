# COMPILADOR 

##  Descripción General

Este proyecto implementa un **compilador** diseñado para controlar las articulaciones (motores) del brazo robotico realizado en 
**CoppeliaSim**. El sistema permite escribir código de alto nivel para controlar los movimientos de un brazo robotico
y generar automáticamente código Lua ejecutable en el simulador.

##  Características Principales

###  **Analizador Léxico y Sintáctico**
- **Lexer generado con JFlex**: Reconoce tokens del DSL (palabras clave, identificadores, números, operadores)
- **Parser sintáctico personalizado**: Valida la gramática del DSL y detecta errores sintácticos y semánticos
- **Tabla de símbolos**: Gestiona identificadores de robots, métodos y valores
- **Validación semántica**: Verifica rangos de movimiento, declaración de variables y secuencias válidas

###  **Scrips para ingresar en la interfaz grafica del programa**
```java
Robot r1                    // Declaración de robot
r1.iniciar                  // Inicialización
r1.base = 90               // Movimiento de articulación (0-360°)
r1.velocidad = 30          // Velocidad de movimiento (1-60°/s)
r1.repetir = 2 {           // Bucles de repetición
    r1.garra = 45
    r1.velocidad = 25
}
r1.finalizar               // Finalización
```

###  **Interfaz Gráfica**
- **Editor de código** con numeración de líneas y resaltado de errores
- **Tabla de tokens** léxicos generados
- **Tabla de símbolos** con identificadores y valores
- **Tabla de código intermedio** (Three Address Code/Cuádruplas)
- **Panel de errores** sintácticos y semánticos
- **Generación automática** de ASM, obj, .exe y lua


##  Casos de Uso Soportados

### **1. Secuencias Lineales**
```java
Robot r1
r1.iniciar
r1.garra = 30
r1.velocidad = 40
r1.base = 90
r1.velocidad = 50
r1.finalizar
```

### **2. Secuencias Complejas**
```java
Robot r3
r3.iniciar
r3.base = 180
r3.velocidad = 30
r3.hombro = 90
r3.velocidad = 25
r3.garra = 60
r3.velocidad = 35
r3.finalizar
```

##  Tecnologías Utilizadas

- **Java 17+**: Lenguaje principal del compilador
- **JFlex**: Generador de analizadores léxicos
- **Java Swing**: Interfaz gráfica de usuario
- **CoppeliaSim**: Simulador de robots industriales
- **Lua**: Lenguaje de scripting de CoppeliaSim
- **TASM/DOSBox**: Ensamblador y emulador para código ASM

##  Flujo de Ejecución

1. **Escritura de código DSL** en el editor
2. **Análisis léxico** → Generación de tokens
3. **Análisis sintáctico** → Validación y tabla de símbolos
4. **Generación de código intermedio** → Cuádruplas, codigo ASM, OBJ, EXE
5. **Traducción a Lua** → Script para CoppeliaSim
6. **Ejecución en simulador** → Movimientos del robot
