// Campo de formulário padrão dos modais do painel admin (label + input).
// Estilos em styles/shared.css (.adminForm .formField / .fieldLabel).
export default function FormField({ label, children }) {
    return (
        <div className="formField">
            {label && <label className="fieldLabel">{label}</label>}
            {children}
        </div>
    );
}
