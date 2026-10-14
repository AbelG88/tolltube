# App "Peaje Cognitivo" — Documento de Proyecto

## Idea central

Una app Android que interrumpe periódicamente el uso de YouTube (u otras apps de video) y exige al niño resolver un ejercicio didáctico antes de poder continuar viendo. En vez de bloquear el acceso, convierte el tiempo de pantalla en algo condicionado a un esfuerzo académico.

Pensada inicialmente para un niño de 7 años con autismo, con el objetivo de:
- Reducir el consumo pasivo de pantalla sin generar conflicto abrupto
- Introducir práctica académica dentro de una rutina que el niño ya tiene
- Filtrar contenido inapropiado o estimulante en exceso

---

## Objetivos

### Objetivo principal
Validar que el niño acepta el "peaje" (overlay + ejercicio) y lo resuelve sin crisis, antes de construir cualquier otra funcionalidad.

### Objetivos secundarios
1. Filtrar videos por metadatos (título, canal, tags) usando IA para bloquear contenido no deseado
2. Controlar el tiempo de uso diario por app
3. Funcionar en dos dispositivos: el del niño (modo restrictivo) y el del padre (perfil separado)

---

## Funcionalidades

### MVP — Fase 1: Overlay con ejercicio
- Cada N minutos, aparece un overlay encima de YouTube
- El overlay muestra un ejercicio didáctico (leer una palabra, resolver una cuenta, escribir lo que ve en una imagen, etc.)
- El niño debe responder correctamente para que el overlay desaparezca y pueda seguir viendo
- Los ejercicios son configurables por el padre según lo que el niño esté aprendiendo en ese momento
- La IA valida la respuesta (texto, voz o selección múltiple según el ejercicio)

### Fase 2: Clasificación de video
- Al detectar que se está reproduciendo un video en YouTube, la app consulta la **YouTube Data API v3** con el ID del video
- Obtiene: título, canal, descripción, tags, categoría, thumbnail
- Un modelo de IA clasifica el video según reglas configuradas por el padre (ej: "bloquear videos de efectos estridentes", "bloquear canales X e Y")
- Si el video no pasa el filtro, aparece una imagen encima que no se puede cerrar (o se fuerza el cierre del video)

### Fase 3: Control de tiempo
- Tiempo máximo diario por app configurable
- Notificación previa al límite ("te quedan 10 minutos")
- Al llegar al límite, bloqueo con mensaje amigable
- Integración con Digital Wellbeing API de Android o implementación propia si se necesita más control

---

## Stack técnico

| Componente | Tecnología |
|---|---|
| App Android | Kotlin (o Flutter si se quiere multiplataforma después) |
| Overlay sobre apps | Accessibility Service + SYSTEM_ALERT_WINDOW |
| Detección de video activo | Accessibility Service (detecta app en primer plano y URL activa) |
| Clasificación de video | YouTube Data API v3 + modelo de IA (GPT/Claude/Gemini) |
| Ejercicios y validación | IA generativa con prompt adaptado al nivel del niño |
| Reconocimiento de voz (opcional) | SpeechRecognizer de Android |
| Backend | Mínimo o ninguno en v1; puede ser Firebase para sync entre dispositivos |

---

## Configuración de dispositivos

### Dispositivo del niño
- Perfil Android en modo kiosk o launcher propio
- La app corre siempre en primer plano
- Sin acceso a configuraciones del sistema

### Dispositivo del padre (Abel)
- Perfil de usuario separado "modo niño" en Android
- Al entregar el celu, cambiar al perfil del niño
- No afecta el uso normal del padre

---

## Plan de desarrollo

### Paso 1 — Validación (antes de escribir código)
- Definir qué tipos de ejercicios se usarán en el MVP
- Decidir cómo responde el niño: selección múltiple, voz, texto escrito
- Definir la frecuencia inicial del overlay (cada 10 min, cada 15 min, etc.)
- Decidir si la IA valida en tiempo real o si los ejercicios son predefinidos

### Paso 2 — Prototipo del overlay
- App Android básica con Accessibility Service activado
- Overlay que aparece cada N minutos sobre cualquier app
- Un ejercicio hardcodeado para probar con el niño
- Sin IA todavía, solo validar que el overlay funciona y que el niño lo tolera

### Paso 3 — Ejercicios con IA
- Integrar API de IA para generar ejercicios según nivel configurado
- Validación de respuesta por IA
- Panel de configuración para el padre: nivel, tipo de ejercicio, frecuencia

### Paso 4 — Clasificación de video
- Integrar YouTube Data API v3
- Definir reglas de filtro (canales bloqueados, palabras clave en título, categorías)
- Implementar el bloqueo de video con overlay permanente

### Paso 5 — Control de tiempo
- Implementar contador de uso por app
- Configuración del límite diario
- Notificaciones y bloqueo final

### Paso 6 — Multi-dispositivo
- Sincronización de configuración entre dispositivos (Firebase o similar)
- Historial de ejercicios resueltos para seguimiento

---

## Consideraciones para escalar

Si el prototipo funciona bien con el niño y se quiere convertir en producto:
- UX pensada para padres no técnicos (onboarding simple, configuración guiada)
- Posibilidad de conectar con el currículum escolar real (qué está aprendiendo el niño ese mes)
- Soporte para múltiples perfiles de hijos
- Monetización: freemium con límite de ejercicios por día, o suscripción mensual
- Comunidad de padres para compartir sets de ejercicios

---

## Próximo paso acordado

Arrancar por el **prototipo del overlay** (Paso 2), con un ejercicio simple hardcodeado, para validar si el niño lo acepta y lo resuelve antes de invertir más desarrollo.

---

## Estado actual

Ver [diagnostico-intermitencia.md](diagnostico-intermitencia.md): el overlay funciona de forma intermitente en el celular de pruebas y se está investigando por qué, con logs de debug.
