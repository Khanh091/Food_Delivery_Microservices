import { useNavigate } from 'react-router-dom'
import { SearchForm } from '../features/search/components/SearchForm'

export function HomePage() {
  const navigate = useNavigate()

  return (
    <main className="home-page">
      <section className="home-hero">
        <p className="eyebrow">Food Delivery</p>
        <h1>Hôm nay bạn muốn ăn gì?</h1>
        <p>Tìm món ăn, nhà hàng hoặc chi nhánh phù hợp với bạn.</p>
        <SearchForm onSearch={(query) => navigate(`/search?q=${encodeURIComponent(query)}`)} />
      </section>
    </main>
  )
}
