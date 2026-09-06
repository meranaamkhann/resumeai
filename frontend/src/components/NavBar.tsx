import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function NavBar() {
  const { isAuthenticated, fullName, logout } = useAuth()

  return (
    <nav className="navbar">
      <Link to="/" className="brand">Resume Intelligence</Link>
      {isAuthenticated ? (
        <div className="nav-links">
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/upload">Analyze</Link>
          <Link to="/applications">Applications</Link>
          <span>{fullName}</span>
          <button onClick={logout}>Log out</button>
        </div>
      ) : (
        <div className="nav-links">
          <Link to="/login">Log in</Link>
          <Link to="/register">Register</Link>
        </div>
      )}
    </nav>
  )
}
