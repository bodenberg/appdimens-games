# Performance e medição

AppDimens Games otimiza a frequência e o formato do trabalho: batch contíguo, configuração
imutável e ausência de JNI por item. Não são publicados números universais, pois dispositivos,
ART, compilador NDK, estado térmico e governor alteram a medição.

## Protocolo recomendado

* use release, warm-up e afinidade consistente;
* meça scalar e lotes de 16/64/1024 separadamente;
* reporte mediana, p95, compilador, ABI, modelo e temperatura;
* compare o custo total de atualização de viewport, não um loop eliminado pelo compilador;
* confirme o resultado com checksum para impedir dead-code elimination.

Para o frame, mantenha os valores calculados em arrays/SOA da engine. Recalcule somente em
eventos de superfície. Em JNI, lotes amortizam pin/copy e a transição de runtime.
