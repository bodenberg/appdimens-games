# Paridade com AppDimens Dynamic 3.1.6

Este porte foi reimplementado com um contrato público deliberadamente paralelo ao Dynamic; o código legado não é o motor desta API.

| Dynamic | Games | Propriedades Compose |
|---|---|---|
| `appdimens-dynamic` | `appdimens-games-compose` | `sdp`, `hdp`, `wdp`, `ssp`, `hsp`, `wsp` |
| `-auto` | `-auto` | `asdp`, `ashdp`, `aswdp`, `assp` |
| `-density` | `-density` | `dsdp`, `dshdp`, `dswdp`, `dssp` |
| `-diagonal` | `-diagonal` | `dgsdp`, `dgshdp`, `dgswdp`, `dgssp` |
| `-fill` | `-fill` | `flsdp`, `flshdp`, `flswdp`, `flssp` |
| `-fit` | `-fit` | `ftsdp`, `ftshdp`, `ftswdp`, `ftssp` |
| `-fluid` | `-fluid` | `fsdp`, `fshdp`, `fswdp`, `fssp` |
| `-interpolated` | `-interpolated` | `isdp`, `ishdp`, `iswdp`, `issp` |
| `-logarithmic` | `-logarithmic` | `logsdp`, `logshdp`, `logswdp`, `logssp` |
| `-percent` | `-percent` | `psdp`, `pshdp`, `pswdp`, `pssp` |
| `-perimeter` | `-perimeter` | `prsdp`, `prshdp`, `prswdp`, `prssp` |
| `-power` | `-power` | `pwsdp`, `pwshdp`, `pwswdp`, `pwssp` |
| `-resize` | `-resize` | `rsdp`, `rshdp`, `rswdp`, `rssp` |

Os conceitos comuns mantêm os nomes `DpQualifier`, `DpQualifierEntry`, `Inverter`, `Orientation` e `UnitType`. Funções explícitas trocam somente o nome do produto: por exemplo, `toDynamicFitDp` torna-se `toGameFitDp`.

## Hot path

`GameDimens.calculate` é puro, sem estado e sem alocação. A sobrecarga de vetor trabalha *in-place*, evitando uma travessia JNI por elemento. Compose deve ser usado para layout; engines Vulkan/OpenGL devem chamar o núcleo Java em lote ou a ABI C do módulo native fora do comando de renderização.
