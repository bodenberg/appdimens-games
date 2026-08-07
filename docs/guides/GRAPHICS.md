# Vulkan, OpenGL ES e engines

## OpenGL ES

Converta `Viewport.Transform` com `GraphicsViewport.openGl`, arredonde pelos métodos `pixel*`
e chame `glViewport` na render thread. A biblioteca não modifica estado GL.

## Vulkan

Use `GraphicsViewport.vulkan` ao preencher `VkViewport`. O adapter usa altura positiva;
engines que invertem Y devem ajustar `y/height` conforme a convenção de seu pipeline.
Recalcule após recriar o swapchain.

## Gêneros

* FPS: FILL no mundo, DIAGONAL no crosshair/touch e DEFAULT no HUD.
* Mundo aberto: BALANCED para marcadores e FILL para fundos/mapa.
* Roguelike/estratégia: FIT para o tabuleiro e FLUID para texto.
* Corrida: BALANCED em veículos, DEFAULT em telemetria e FIT no minimapa.
* TV: LOGARITHMIC em overlays e constraints de acessibilidade em texto.
