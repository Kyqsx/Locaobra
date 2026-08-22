// Mapa de rotas administrativas permitidas por cargo de funcionário.
// ADMIN sempre tem acesso total (tratado à parte em canAccessAdminRoute).
// '/admin' (dashboard) mostra dados sensíveis (lista de usuários, indicadores
// financeiros) e por isso é restrito a ADMIN e GERENTE_OPERACOES.
export const ADMIN_ALLOWED_PATHS = {
  GERENTE_OPERACOES: [
    '/admin/**',
    '/admin',
    '/admin/equipamentos',
    '/admin/expedicao',
    '/admin/ordens-servico',
    '/admin/clientes',
    '/admin/funcionarios',
    '/admin/notificacoes',
    '/admin/cargos',
    '/admin/departamentos',
    '/admin/pedidos',
  ],
  RH: ['/admin/clientes', '/admin/funcionarios', '/admin/cargos', '/admin/departamentos'],
  TECNICO_MANUTENCAO: ['/admin/equipamentos', '/admin/ordens-servico'],
  ENTREGADOR: ['/admin/expedicao'],
  CONFERENTE: ['/admin/expedicao', '/admin/equipamentos', '/admin/pedidos'],
  ANALISTA_FINANCEIRO: ['/admin/notificacoes'],
  CONSULTOR_LOCACAO: ['/admin/clientes', '/admin/expedicao', '/admin/equipamentos', '/admin/pedidos'],
  ANALISTA_CREDENCIAMENTO: ['/admin/clientes', '/admin/pedidos'],
  FAXINEIRO: ['/admin/equipamentos'],
};

// Primeira rota que faz sentido abrir para cada cargo depois do login,
// usada para redirecionar quando o usuário cai em '/admin' sem ter
// permissão de ver o dashboard.
export function getDefaultAdminPath(user) {
  if (!user) return '/';
  if (user.tipo === 'ADMIN') return '/admin';
  if (user.tipo !== 'FUNCIONARIO') return '/';

  const allowed = ADMIN_ALLOWED_PATHS[user.cargoFuncionario] || [];
  return allowed[0] || '/';
}

export function canAccessAdminRoute(user, pathname = '/') {
  if (!user) return false;

  if (user.tipo === 'ADMIN') {
    return true;
  }

  if (user.tipo !== 'FUNCIONARIO') {
    return false;
  }

  const normalizedPath = pathname.replace(/\/+$/, '') || '/';
  const cargo = user.cargoFuncionario;
  const allowedPaths = ADMIN_ALLOWED_PATHS[cargo] || [];

  return allowedPaths.some(path => {
    const normalizedAllowedPath = path.replace(/\/+$/, '') || '/';
    return normalizedPath === normalizedAllowedPath || normalizedPath.startsWith(`${normalizedAllowedPath}/`);
  });
}