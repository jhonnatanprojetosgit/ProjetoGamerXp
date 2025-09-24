document.addEventListener('DOMContentLoaded', () => {

    const searchInput = document.getElementById('searchInput');
    const gamesResultsContainer = document.getElementById('gamesResults');

    // --- PASSO 1: Adicionamos a propriedade "link" em cada jogo ---
    const allGames = [
        { 
            name: 'God of War Ragnarok', 
            price: 'R$ 150,00', 
            image: 'imagens-lancamentos/1.jpg',
            link: 'https://store.steampowered.com/app/2322010/God_of_War_Ragnark/'
        },
        { 
            name: 'Watch Dogs Legion', 
            price: 'Gratuito', 
            image: 'imagens-lancamentos/2.webp',
            link: 'https://store.steampowered.com/app/2239550/Watch_Dogs_Legion/'
        },
        { 
            name: 'Assassins Creed Valhalla', 
            price: 'R$ 100,00', 
            image: 'imagens-lancamentos/3.jpg',
            link: 'https://store.steampowered.com/app/2208920/Assassins_Creed_Valhalla/'
        },
        { 
            name: 'Far Cry 6', 
            price: 'R$ 80,00', 
            image: 'imagens-lancamentos/4.jfif',
            link: 'https://store.steampowered.com/app/2369390/Far_Cry_6/'
        },
        { 
            name: 'DOOM', 
            price: 'R$ 200,00', 
            image: 'imagens-lancamentos/5.jpg',
            link: 'https://store.steampowered.com/app/379720/DOOM/'
        },
        { 
            name: 'Tomb Raider', 
            price: 'Gratuito', 
            image: 'imagens-lancamentos/6.jpg',
            link: 'https://store.steampowered.com/app/203160/Tomb_Raider/'
        },
        { 
            name: 'Assassins Creed Shadows', 
            price: 'R$ 90,00', 
            image: 'imagens-lancamentos/7.avif',
            link: 'https://www.xbox.com/pt-BR/games/store/assassins-creed-shadows/9p42lwgmc7k4'
        },
        { 
            name: 'Streetwise', 
            price: 'R$ 120,00', 
            image: 'imagens-lancamentos/8.jpg',
            link: 'https://lista.mercadolivre.com.br/streetwise'
        },
        { 
            name: 'Zelda', 
            price: 'Gratuito', 
            image: 'imagens-lancamentos/9.png',
            link: 'https://store.steampowered.com/tags/en/Zelda?l=portuguese'
        },
        { 
            name: 'Bloodborne', 
            price: 'R$ 50,00', 
            image: 'imagens-lancamentos/10.jpg',
            link: 'https://store.steampowered.com/curator/78702-BLOODBORNE-PC/?l=portuguese'
        },
        { 
            name: 'Jogo 11', 
            price: 'R$ 180,00', 
            image: 'imagens-lancamentos/11.jpg',
            link: '#' // Link de exemplo, já que não havia na index.html
        },
        { 
            name: 'Metal Gear Solid', 
            price: 'R$ 75,00', 
            image: 'imagens-lancamentos/12.webp',
            link: 'https://store.steampowered.com/app/2417610/METAL_GEAR_SOLID_D_SNAKE_EATER/'
        },
    ];
    
    function createGameCard(game) {
        const card = document.createElement('div');
        card.className = 'game-card';
        // --- PASSO 2: A imagem agora é envolvida por uma tag <a> ---
        card.innerHTML = `
            <div class="placeholder">
                <a href="${game.link}" target="_blank">
                    <img src="${game.image}" alt="${game.name}">
                </a>
            </div>
            <div class="game-info">
                <h3>${game.name}</h3>
                <span class="game-price">${game.price}</span>
            </div>
        `;
        return card;
    }

    function renderGames(games) {
        gamesResultsContainer.innerHTML = '';
        if (games.length === 0) {
            gamesResultsContainer.innerHTML = '<p style="color: #fff; text-align: center; width: 100%;">Nenhum jogo encontrado.</p>';
        } else {
            games.forEach(game => {
                gamesResultsContainer.appendChild(createGameCard(game));
            });
        }
    }

    function handleSearch() {
        const query = searchInput.value.toLowerCase();
        const filteredGames = allGames.filter(game => 
            game.name.toLowerCase().includes(query)
        );
        renderGames(filteredGames);
    }

    searchInput.addEventListener('input', handleSearch);

    // Renderiza todos os jogos inicialmente
    renderGames(allGames);
});