export const ADMIN_ALLOWED_PATHS = {
  RH: ['/admin/clientes', '/admin/funcionarios'],
  TECNICO_MANUTENCAO: ['/admin/equipamentos'],
  ENTREGADOR: ['/admin/expedicao'],
};

export function canAccessAdminRoute(user, pathname = '/') {
  if (!user) return false;

  if (user.tipo === 'ADMIN') {
    return true;
  }

  if (user.tipo !== 'FUNCIONARIO') {
    return false;
  }

  const normalizedPath = pathname.replace(/\/+$/, '') || '/';

  if (normalizedPath === '/admin') {
    return true;
  }

  const cargo = user.cargoFuncionario;
  const allowedPaths = ADMIN_ALLOWED_PATHS[cargo] || [];

  return allowedPaths.some(path => {
    const normalizedAllowedPath = path.replace(/\/+$/, '') || '/';
    return normalizedPath === normalizedAllowedPath || normalizedPath.startsWith(`${normalizedAllowedPath}/`);
  });
}
