// Um item numerado dentro do corpo do artigo
// (ex: "1. Martelete Demolidor... Esqueça a marreta manual...")
class ArtigoItem {
  final String titulo;
  final String descricao;

  const ArtigoItem({required this.titulo, required this.descricao});
}

// Modelo de artigo, compartilhado pela home (preview), pela listagem
// "Dicas LocaObra" e pela tela de detalhe do artigo.
class Artigo {
  final String slug;
  final String imagePath;
  final String titulo;
  final String resumo; // texto curto usado nos cards
  final String autor;
  final String data; // já formatada, ex: "17/09/2026"
  final String introducao; // parágrafo de abertura do artigo completo
  final List<ArtigoItem> itens; // itens numerados do corpo
  final String? dicaSeguranca; // caixa de aviso no final (opcional)

  const Artigo({
    required this.slug,
    required this.imagePath,
    required this.titulo,
    required this.resumo,
    required this.autor,
    required this.data,
    required this.introducao,
    this.itens = const [],
    this.dicaSeguranca,
  });
}

// Lista de artigos disponíveis.
// TODO: substituir por dados reais (de uma API, CMS, etc).
const List<Artigo> artigosDisponiveis = [
  Artigo(
    slug: 'derrubar-parede-e-integrar-ambientes',
    imagePath: 'assets/imagens/img_art1.jpg',
    titulo: 'Derrubar Parede e Integrar Ambientes: O Guia Sem Risco',
    resumo:
        'Descubra quais itens não podem faltar no seu canteiro para evitar atrasos...',
    autor: 'Equipe LocaObra',
    data: '17/09/2026',
    introducao:
        'Quer criar um conceito aberto na sua casa? Descubra como demolir alvenaria de forma '
        'rápida, segura e limpa utilizando as ferramentas certas.',
    itens: [
      ArtigoItem(
        titulo: 'Martelete Demolidor de 10 kg a 15 kg',
        descricao:
            'Esqueça a marreta manual, que exige esforço físico extremo e pode trincar o teto '
            'por causa do impacto descontrolado. O martelete elétrico de médio porte realiza o '
            'trabalho em uma fração do tempo, quebrando tijolos e blocos com extrema precisão e '
            'rapidez.',
      ),
      ArtigoItem(
        titulo: 'Lixadeira de Parede e Teto com Aspirador Acoplado',
        descricao:
            'Após a remoção da parede, será preciso emassar e dar o acabamento no contorno de '
            'gesso ou alvenaria. A lixadeira giratória acoplada ao aspirador industrial remove as '
            'imperfeições sem levantar aquela nuvem de poeira que se espalha pela casa inteira.',
      ),
      ArtigoItem(
        titulo: 'Carrinho de Mão Reforçado e Sacos de Entulho de Alta Gramatura',
        descricao:
            'O volume de entulho gerado na remoção de uma única parede costuma surpreender. Ter '
            'sacos reforçados para alvenaria e um carrinho com pneu calibrado facilita o '
            'transporte rápido do resíduo até a caçamba descartável.',
      ),
    ],
    dicaSeguranca:
        'Antes de iniciar a demolição, desligue o disjuntor do circuito elétrico e feche o '
        'registro de água que passam por aquela parede. Utilize sempre óculos de proteção, '
        'luvas e abafador de ruído durante a operação do martelete.',
  ),
  Artigo(
    slug: 'reforma-do-banheiro-ou-cozinha',
    imagePath: 'assets/imagens/img_art2.jpg',
    titulo: 'Reforma do Banheiro ou Cozinha: O Checklist Sem Mistério',
    resumo:
        'Planejar o tempo de uso pode reduzir custos em até 30% no seu projeto final...',
    autor: 'Equipe LocaObra',
    data: '17/09/2026',
    introducao:
        'Vai trocar pisos, azulejos ou tubulações antigas? Veja quais equipamentos alugar para '
        'agilizar sua obra, economizar tempo e evitar retrabalho.',
    itens: [
      ArtigoItem(
        titulo: 'Martelete Rompedor Compacto',
        descricao:
            'Ideal para remover revestimentos antigos e contrapiso sem danificar a estrutura da '
            'laje, rende muito mais que ferramentas manuais em espaços pequenos como banheiros.',
      ),
      ArtigoItem(
        titulo: 'Cortadora de Piso e Azulejo',
        descricao:
            'Garante cortes retos e precisos em porcelanato e cerâmica, essencial para um '
            'acabamento profissional nos cantos e rodapés.',
      ),
      ArtigoItem(
        titulo: 'Nível a Laser',
        descricao:
            'Evita desníveis no piso e nas fiadas de azulejo, um dos erros mais comuns em '
            'reformas feitas sem o equipamento adequado.',
      ),
    ],
    dicaSeguranca:
        'Feche o registro geral de água antes de mexer em qualquer tubulação, e use óculos de '
        'proteção ao operar a cortadora de piso.',
  ),
];