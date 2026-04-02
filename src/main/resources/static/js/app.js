document.addEventListener('DOMContentLoaded', function () {

    // === Busca de CEP ===
    const cepInput = document.getElementById('cep');
    if (cepInput) {
        cepInput.addEventListener('blur', async function () {
            const cep = this.value.replace(/\D/g, '');
            if (cep.length === 8) {
                const spinner = document.getElementById('cep-spinner');
                if (spinner) spinner.style.display = 'inline-block';

                try {
                    const response = await fetch('/api/cep/' + cep);
                    if (response.ok) {
                        const data = await response.json();
                        document.getElementById('logradouro').value = data.logradouro || '';
                        document.getElementById('bairro').value = data.bairro || '';
                        document.getElementById('cidade').value = data.localidade || '';
                        document.getElementById('uf').value = data.uf || '';
                    } else {
                        alert('CEP não encontrado.');
                    }
                } catch (error) {
                    console.error('Erro ao buscar CEP:', error);
                } finally {
                    if (spinner) spinner.style.display = 'none';
                }
            }
        });
    }

    // === Auto-dismiss de toasts ===
    const toasts = document.querySelectorAll('.toast');
    toasts.forEach(function (toast) {
        setTimeout(function () {
            toast.classList.add('toast-hide');
            setTimeout(function () {
                toast.remove();
            }, 400);
        }, 4000);
    });

    // === Modal de confirmação para desativar ===
    const desativarForms = document.querySelectorAll('.form-desativar');
    const modal = document.getElementById('modal-confirmacao');
    const btnCancelar = document.getElementById('modal-cancelar');
    const btnConfirmar = document.getElementById('modal-confirmar');
    let formAtual = null;

    desativarForms.forEach(function (form) {
        form.addEventListener('submit', function (event) {
            event.preventDefault();
            formAtual = this;
            if (modal) {
                modal.classList.add('modal-ativo');
            }
        });
    });

    if (btnCancelar) {
        btnCancelar.addEventListener('click', function () {
            if (modal) modal.classList.remove('modal-ativo');
            formAtual = null;
        });
    }

    if (btnConfirmar) {
        btnConfirmar.addEventListener('click', function () {
            if (formAtual) {
                formAtual.submit();
            }
        });
    }

    if (modal) {
        modal.addEventListener('click', function (event) {
            if (event.target === modal) {
                modal.classList.remove('modal-ativo');
                formAtual = null;
            }
        });
    }
});
