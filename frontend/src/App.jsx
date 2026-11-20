
import { useEffect, useState } from 'react'
import './App.css'

const API_BASE_URL = 'http://localhost:8080/api'

const roles = [
  {
    value: 'UNIVERSITY_ADMIN',
    title: 'University Admin',
    description: 'Manage and issue academic credentials',
    icon: '🏛️',
  },
  {
    value: 'STUDENT',
    title: 'Student',
    description: 'Access and share your credentials',
    icon: '🎓',
  },
  {
    value: 'EMPLOYER',
    title: 'Employer',
    description: 'Verify academic credentials',
    icon: '💼',
  },
]

const emptyCredentialForm = {
  credentialId: '',
  studentName: '',
  studentEmail: '',
  universityName: 'Demo University',
  degreeName: '',
  department: '',
  graduationYear: '',
  issueDate: new Date().toLocaleDateString('en-CA'),
}

function formatDate(value) {
  if (!value) return 'Not available'

  const date = new Date(`${value}T00:00:00`)

  if (Number.isNaN(date.getTime())) return value

  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;')
}

function App() {
  const [selectedRole, setSelectedRole] = useState('UNIVERSITY_ADMIN')
  const [isRegistering, setIsRegistering] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState('')
  const [messageType, setMessageType] = useState('error')

  const [user, setUser] = useState(() => {
    try {
      const savedUser = localStorage.getItem('credentialUser')
      return savedUser ? JSON.parse(savedUser) : null
    } catch {
      return null
    }
  })

  const [activeView, setActiveView] = useState('dashboard')

  // Admin credential management
  const [credentials, setCredentials] = useState([])
  const [credentialsLoading, setCredentialsLoading] = useState(false)
  const [credentialsError, setCredentialsError] = useState('')
  const [credentialForm, setCredentialForm] = useState(emptyCredentialForm)
  const [creatingCredential, setCreatingCredential] = useState(false)
  const [issuingCredentialId, setIssuingCredentialId] = useState('')
  const [revokingCredentialId, setRevokingCredentialId] = useState('')
  const [managementMessage, setManagementMessage] = useState('')
  const [managementMessageType, setManagementMessageType] = useState('success')

  // Student credentials and individual credential details
  const [studentCredentials, setStudentCredentials] = useState([])
  const [studentCredentialsLoading, setStudentCredentialsLoading] = useState(false)
  const [studentCredentialsError, setStudentCredentialsError] = useState('')
  const [selectedCredential, setSelectedCredential] = useState(null)
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')

  // Employer verification
  const [verificationId, setVerificationId] = useState('')
  const [verificationLoading, setVerificationLoading] = useState(false)
  const [verificationResult, setVerificationResult] = useState(null)
  const [verificationError, setVerificationError] = useState('')

  // Live blockchain connection status
  const [blockchainStatus, setBlockchainStatus] = useState(null)
  const [blockchainLoading, setBlockchainLoading] = useState(false)
  const [blockchainError, setBlockchainError] = useState('')
  const [blockchainLastChecked, setBlockchainLastChecked] = useState(null)

  const showMessage = (text, type = 'error') => {
    setMessage(text)
    setMessageType(type)
  }

  const apiRequest = async (path, options = {}) => {
    if (!user?.accessToken) {
      throw new Error('Your session has expired. Please sign in again.')
    }

    const response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${user.accessToken}`,
        ...options.headers,
      },
    })

    const data = await response.json().catch(() => ({}))

    if (!response.ok) {
      throw new Error(
        data.message ||
        data.error ||
        `Request failed (${response.status}). Please try again.`
      )
    }

    return data
  }

  // Load live blockchain connection information
  const loadBlockchainStatus = async () => {
    setBlockchainLoading(true)
    setBlockchainError('')

    try {
      const data = await apiRequest('/blockchain/status')

      setBlockchainStatus(data)
    } catch (error) {
      setBlockchainStatus(null)
      setBlockchainError(error.message || 'Unable to connect to the blockchain node.')
    } finally {
      setBlockchainLastChecked(new Date())
      setBlockchainLoading(false)
    }
  }

  // Load all credentials for University Admin
  const loadCredentials = async () => {
    setCredentialsLoading(true)
    setCredentialsError('')

    try {
      const data = await apiRequest('/credentials')
      setCredentials(Array.isArray(data) ? data : [])
    } catch (error) {
      setCredentialsError(error.message)
    } finally {
      setCredentialsLoading(false)
    }
  }

  // Load only the logged-in student's credentials
  const loadStudentCredentials = async () => {
    setStudentCredentialsLoading(true)
    setStudentCredentialsError('')

    try {
      const data = await apiRequest('/credentials/my')
      setStudentCredentials(Array.isArray(data) ? data : [])
    } catch (error) {
      setStudentCredentialsError(error.message)
    } finally {
      setStudentCredentialsLoading(false)
    }
  }

  // Fetch one credential using the existing protected backend endpoint
  const openCredentialDetails = async (credentialId) => {
    setActiveView('credential-details')
    setSelectedCredential(null)
    setDetailError('')
    setDetailLoading(true)

    try {
      const data = await apiRequest(
        `/credentials/${encodeURIComponent(credentialId)}`
      )

      setSelectedCredential(data)
    } catch (error) {
      setDetailError(error.message)
    } finally {
      setDetailLoading(false)
    }
  }

  useEffect(() => {
    if (user?.role === 'UNIVERSITY_ADMIN') {
      loadCredentials()
      loadBlockchainStatus()
    }
  }, [user?.role, user?.accessToken])

  const handleSubmit = async (event) => {
    event.preventDefault()
    setMessage('')

    if (!email.trim() || !password) {
      showMessage('Please enter your email address and password.')
      return
    }

    if (isRegistering && password !== confirmPassword) {
      showMessage('The passwords do not match.')
      return
    }

    if (isRegistering && selectedRole === 'UNIVERSITY_ADMIN') {
      showMessage('University Admin accounts cannot be created through public registration.')
      return
    }

    setLoading(true)

    try {
      const endpoint = isRegistering ? '/auth/register' : '/auth/login'

      const requestBody = isRegistering
        ? {
            email: email.trim(),
            password,
            role: selectedRole,
          }
        : {
            email: email.trim(),
            password,
          }

      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestBody),
      })

      const data = await response.json().catch(() => ({}))

      if (!response.ok) {
        throw new Error(
          data.message ||
          data.error ||
          (response.status === 401
            ? 'Invalid email or password.'
            : `Request failed (${response.status}). Please try again.`)
        )
      }

      if (isRegistering) {
        showMessage('Account created successfully. You can now sign in.', 'success')
        setIsRegistering(false)
        setPassword('')
        setConfirmPassword('')
        return
      }

      if (!data.accessToken || !data.role) {
        throw new Error('The server returned an incomplete login response.')
      }

      if (data.role !== selectedRole) {
        throw new Error(
          `This account belongs to the ${data.role.replaceAll('_', ' ')} role. Please select the correct role.`
        )
      }

      const loggedInUser = {
        email: data.email || email.trim(),
        role: data.role,
        accessToken: data.accessToken,
      }

      localStorage.setItem('credentialUser', JSON.stringify(loggedInUser))
      setUser(loggedInUser)
      setPassword('')
      setActiveView('dashboard')
      showMessage('')
    } catch (error) {
      showMessage(
        error.message ||
        'Unable to connect to the server. Make sure the backend is running.'
      )
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('credentialUser')
    setUser(null)
    setEmail('')
    setPassword('')
    setConfirmPassword('')
    setSelectedRole('UNIVERSITY_ADMIN')
    setIsRegistering(false)
    setActiveView('dashboard')

    setCredentials([])
    setStudentCredentials([])
    setStudentCredentialsError('')
    setSelectedCredential(null)
    setDetailError('')

    setVerificationId('')
    setVerificationResult(null)
    setVerificationError('')

    setBlockchainStatus(null)
    setBlockchainError('')
    setBlockchainLastChecked(null)

    setMessage('')
  }

  const handleCredentialFormChange = (event) => {
    const { name, value } = event.target

    setCredentialForm((previous) => ({
      ...previous,
      [name]: value,
    }))
  }

  const handleCreateCredential = async (event) => {
    event.preventDefault()
    setManagementMessage('')
    setCreatingCredential(true)

    try {
      const requestBody = {
        ...credentialForm,
        graduationYear: Number(credentialForm.graduationYear),
      }

      await apiRequest('/credentials', {
        method: 'POST',
        body: JSON.stringify(requestBody),
      })

      setManagementMessage('Credential created successfully. It is now pending issuance.')
      setManagementMessageType('success')

      setCredentialForm({
        ...emptyCredentialForm,
        issueDate: new Date().toLocaleDateString('en-CA'),
      })

      await loadCredentials()
    } catch (error) {
      setManagementMessage(error.message)
      setManagementMessageType('error')
    } finally {
      setCreatingCredential(false)
    }
  }

  const handleIssueCredential = async (credentialId) => {
    const confirmed = window.confirm(
      `Are you sure you want to issue credential ${credentialId} on the blockchain?`
    )

    if (!confirmed) return

    setIssuingCredentialId(credentialId)
    setManagementMessage('')

    try {
      await apiRequest(
        `/credentials/${encodeURIComponent(credentialId)}/issue`,
        { method: 'PUT' }
      )

      setManagementMessage(`Credential ${credentialId} issued successfully.`)
      setManagementMessageType('success')
      await loadCredentials()
    } catch (error) {
      setManagementMessage(error.message)
      setManagementMessageType('error')
    } finally {
      setIssuingCredentialId('')
    }
  }

  const handleRevokeCredential = async (credentialId) => {
    const confirmed = window.confirm(
      `Are you sure you want to revoke credential ${credentialId}? This action will also revoke it on the blockchain.`
    )

    if (!confirmed) return

    setRevokingCredentialId(credentialId)
    setManagementMessage('')

    try {
      await apiRequest(
        `/credentials/${encodeURIComponent(credentialId)}/revoke`,
        { method: 'PUT' }
      )

      setManagementMessage(`Credential ${credentialId} revoked successfully.`)
      setManagementMessageType('success')
      await loadCredentials()
    } catch (error) {
      setManagementMessage(error.message)
      setManagementMessageType('error')
    } finally {
      setRevokingCredentialId('')
    }
  }

  // Print a certificate through the browser's print dialog
  const handleDownloadCertificate = () => {
    if (!selectedCredential) return

    const credential = selectedCredential
    const isIssued = credential.status === 'ISSUED'

    const popup = window.open('', '_blank', 'width=1000,height=800')

    if (!popup) {
      setDetailError('The certificate window was blocked. Please allow pop-ups for localhost and try again.')
      return
    }

    const certificateHtml = `
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Certificate - ${escapeHtml(credential.credentialId)}</title>
        <style>
          @page {
            size: A4 landscape;
            margin: 12mm;
          }

          * {
            box-sizing: border-box;
          }

          body {
            margin: 0;
            padding: 24px;
            font-family: Georgia, 'Times New Roman', serif;
            color: #17243b;
            background: #f3f5f9;
          }

          .certificate {
            position: relative;
            min-height: 530px;
            padding: 42px 55px;
            border: 8px double #2457d6;
            background: #fff;
            text-align: center;
          }

          .inner-border {
            position: absolute;
            inset: 9px;
            border: 1px solid #b9c9ed;
            pointer-events: none;
          }

          .brand {
            color: #2457d6;
            font-family: Arial, sans-serif;
            font-size: 15px;
            font-weight: bold;
            letter-spacing: 3px;
            text-transform: uppercase;
          }

          .university {
            margin-top: 18px;
            font-size: 27px;
            font-weight: bold;
          }

          .heading {
            margin-top: 24px;
            color: #2457d6;
            font-size: 35px;
            letter-spacing: 2px;
            text-transform: uppercase;
          }

          .subheading {
            margin-top: 12px;
            font-size: 17px;
            font-style: italic;
          }

          .student {
            margin: 22px 0 12px;
            font-size: 31px;
            font-weight: bold;
          }

          .degree {
            margin-top: 15px;
            font-size: 23px;
            font-weight: bold;
          }

          .department {
            margin-top: 9px;
            font-size: 17px;
          }

          .year {
            margin-top: 13px;
            font-size: 17px;
          }

          .meta {
            margin-top: 30px;
            padding-top: 15px;
            border-top: 1px solid #dce3f0;
            font-family: Arial, sans-serif;
            font-size: 10px;
            line-height: 1.8;
            overflow-wrap: anywhere;
          }

          .status {
            margin-top: 14px;
            font-family: Arial, sans-serif;
            font-size: 13px;
            font-weight: bold;
            color: ${isIssued ? '#087443' : '#b42318'};
          }

          .disclaimer {
            margin-top: 12px;
            color: #69758a;
            font-family: Arial, sans-serif;
            font-size: 10px;
          }

          @media print {
            body {
              padding: 0;
              background: white;
            }
          }
        </style>
      </head>
      <body>
        <div class="certificate">
          <div class="inner-border"></div>
          <div class="brand">CredSure Academic Records</div>
          <div class="university">${escapeHtml(credential.universityName)}</div>
          <div class="heading">Certificate of Achievement</div>
          <div class="subheading">This certificate records that</div>
          <div class="student">${escapeHtml(credential.studentName)}</div>
          <div class="subheading">has been awarded the degree of</div>
          <div class="degree">${escapeHtml(credential.degreeName)}</div>
          <div class="department">
            Department of ${escapeHtml(credential.department)}
          </div>
          <div class="year">
            Graduation Year: ${escapeHtml(credential.graduationYear)}
          </div>
          <div class="meta">
            Credential ID: ${escapeHtml(credential.credentialId)}<br>
            Issue Date: ${escapeHtml(formatDate(credential.issueDate))}<br>
            Certificate SHA-256: ${escapeHtml(credential.certificateHash || 'Not available')}<br>
            Blockchain Transaction: ${escapeHtml(credential.blockchainTransactionHash || 'Not available')}
          </div>
          <div class="status">
            RECORD STATUS: ${escapeHtml(credential.status || 'UNKNOWN')}
          </div>
          <div class="disclaimer">
            Generated by the CredSure academic credential demonstration system.
            This document is a printable record, not a digitally signed university certificate.
          </div>
        </div>
        <script>
          window.onload = function () {
            window.focus();
            window.print();
          };
        </script>
      </body>
      </html>
    `

    popup.document.open()
    popup.document.write(certificateHtml)
    popup.document.close()
  }

  // Employer credential verification
  const handleVerifyCredential = async (event) => {
    event.preventDefault()

    const id = verificationId.trim()

    if (!id) {
      setVerificationError('Please enter a credential ID.')
      setVerificationResult(null)
      return
    }

    setVerificationLoading(true)
    setVerificationResult(null)
    setVerificationError('')

    try {
      const data = await apiRequest(
        `/credentials/verify/${encodeURIComponent(id)}`
      )

      setVerificationResult(data)
    } catch (error) {
      setVerificationError(error.message)
    } finally {
      setVerificationLoading(false)
    }
  }

  const openVerification = () => {
    setActiveView('verify')
    setVerificationResult(null)
    setVerificationError('')
  }

  const activeRole = roles.find((role) => role.value === user?.role)

  if (user) {
    return (
      <div className="app-shell">
        <header className="topbar">
          <a className="brand" href="#home">
            <span className="brand-icon">◆</span>
            <span>Cred<span className="brand-accent">Sure</span></span>
          </a>

          <div className="topbar-right">
            <span className="user-role">{activeRole?.title || user.role}</span>
            <button className="logout-button" onClick={handleLogout}>
              Sign out
            </button>
          </div>
        </header>

        <main className="dashboard">
          <div className="dashboard-welcome">
            <div>
              <p className="eyebrow">CREDENTIAL MANAGEMENT PORTAL</p>
              <h1>Welcome back!</h1>
              <p className="welcome-description">
                Signed in as <strong>{user.email}</strong>
              </p>
            </div>
            <div className="welcome-icon">{activeRole?.icon || '🔐'}</div>
          </div>

          <section className="dashboard-card">
            <div className="dashboard-card-icon">🔒</div>
            <div>
              <h2>{activeRole?.title || 'Your Dashboard'}</h2>
              <p>
                Your account is authenticated. Your role-based workspace is
                ready for the next stage of development.
              </p>
              <span className="status-pill">Authenticated</span>
            </div>
          </section>

          {/* UNIVERSITY ADMIN DASHBOARD */}
          {user.role === 'UNIVERSITY_ADMIN' && (
            <>
              <section className="feature-grid">
                <button
                  type="button"
                  className="feature-card"
                  onClick={() => {
                    setActiveView('manage')
                    setManagementMessage('')
                    loadCredentials()
                  }}
                >
                  <span className="feature-icon">📄</span>
                  <h3>Manage Credentials</h3>
                  <p>Create, issue, and view academic credentials.</p>
                  <strong>Open management →</strong>
                </button>

                <button
                  type="button"
                  className="feature-card"
                  onClick={() => {
                    setActiveView('manage')
                    setManagementMessage('Use the Revoke buttons beside issued credentials below.')
                    setManagementMessageType('success')
                    loadCredentials()
                  }}
                >
                  <span className="feature-icon">🚫</span>
                  <h3>Revoke Credentials</h3>
                  <p>Revoke issued credentials and track their status.</p>
                  <strong>Open revocation →</strong>
                </button>

                {/* LIVE BLOCKCHAIN STATUS CARD */}
                <article className="feature-card blockchain-feature-card">
                  <span className="feature-icon">⛓️</span>
                  <h3>Blockchain Status</h3>
                  <p>Live connection to your local Hardhat blockchain.</p>

                  <div style={blockchainCardContentStyle}>
                    <div style={blockchainStatusRowStyle}>
                      <span
                        style={{
                          ...blockchainIndicatorStyle,
                          background: blockchainLoading
                            ? '#d99a19'
                            : blockchainStatus?.connected
                              ? '#087443'
                              : '#b42318',
                        }}
                      />

                      <strong
                        style={{
                          color: blockchainLoading
                            ? '#8a5a00'
                            : blockchainStatus?.connected
                              ? '#087443'
                              : '#b42318',
                        }}
                      >
                        {blockchainLoading
                          ? 'Checking connection...'
                          : blockchainStatus?.connected
                            ? 'Connected'
                            : 'Disconnected'}
                      </strong>
                    </div>

                    {blockchainStatus?.connected && (
                      <>
                        <div style={blockchainInfoStyle}>
                          <small>Network</small>
                          <strong>{blockchainStatus.network || 'Not available'}</strong>
                        </div>

                        <div style={blockchainInfoStyle}>
                          <small>Client Version</small>
                          <strong style={blockchainValueStyle}>
                            {blockchainStatus.clientVersion || 'Not available'}
                          </strong>
                        </div>

                        <div style={blockchainInfoStyle}>
                          <small>Smart Contract Address</small>
                          <strong style={blockchainValueStyle}>
                            {blockchainStatus.contractAddress || 'Not available'}
                          </strong>
                        </div>

                        <p style={blockchainMessageStyle}>
                          {blockchainStatus.message}
                        </p>
                      </>
                    )}

                    {blockchainError && (
                      <p style={blockchainErrorStyle} role="alert">
                        {blockchainError}
                      </p>
                    )}

                    {blockchainLastChecked && (
                      <p style={blockchainLastCheckedStyle}>
                        Last checked: {blockchainLastChecked.toLocaleTimeString('en-IN')}
                      </p>
                    )}

                    <button
                      type="button"
                      className="submit-button"
                      style={blockchainRefreshButtonStyle}
                      onClick={loadBlockchainStatus}
                      disabled={blockchainLoading}
                    >
                      {blockchainLoading ? 'Checking...' : '↻ Refresh Status'}
                    </button>
                  </div>
                </article>
              </section>

              {activeView === 'manage' && (
                <section style={panelStyle}>
                  <div style={panelHeaderStyle}>
                    <div>
                      <p className="eyebrow">UNIVERSITY ADMIN</p>
                      <h2>Manage Academic Credentials</h2>
                      <p>Create, issue, and revoke academic credentials.</p>
                    </div>

                    <button
                      type="button"
                      className="logout-button"
                      onClick={() => setActiveView('dashboard')}
                    >
                      Close
                    </button>
                  </div>

                  <h3>Create a New Credential</h3>

                  <form onSubmit={handleCreateCredential} style={formGridStyle}>
                    <label style={fieldStyle}>
                      Credential ID *
                      <input
                        name="credentialId"
                        value={credentialForm.credentialId}
                        onChange={handleCredentialFormChange}
                        placeholder="e.g. CRED-2026-053"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Student Name *
                      <input
                        name="studentName"
                        value={credentialForm.studentName}
                        onChange={handleCredentialFormChange}
                        placeholder="Enter student name"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Student Email *
                      <input
                        name="studentEmail"
                        type="email"
                        value={credentialForm.studentEmail}
                        onChange={handleCredentialFormChange}
                        placeholder="student@example.com"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      University Name *
                      <input
                        name="universityName"
                        value={credentialForm.universityName}
                        onChange={handleCredentialFormChange}
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Degree Name *
                      <input
                        name="degreeName"
                        value={credentialForm.degreeName}
                        onChange={handleCredentialFormChange}
                        placeholder="e.g. Bachelor of Technology"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Department *
                      <input
                        name="department"
                        value={credentialForm.department}
                        onChange={handleCredentialFormChange}
                        placeholder="e.g. Computer Engineering"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Graduation Year *
                      <input
                        name="graduationYear"
                        type="number"
                        min="1900"
                        max="2100"
                        value={credentialForm.graduationYear}
                        onChange={handleCredentialFormChange}
                        placeholder="2026"
                        required
                      />
                    </label>

                    <label style={fieldStyle}>
                      Issue Date *
                      <input
                        name="issueDate"
                        type="date"
                        value={credentialForm.issueDate}
                        onChange={handleCredentialFormChange}
                        required
                      />
                    </label>

                    <button
                      className="submit-button"
                      type="submit"
                      disabled={creatingCredential}
                      style={{ gridColumn: '1 / -1' }}
                    >
                      {creatingCredential ? 'Creating...' : 'Create Credential'}
                    </button>
                  </form>

                  {managementMessage && (
                    <p
                      role="alert"
                      style={{
                        ...noticeStyle,
                        color: managementMessageType === 'success' ? '#087443' : '#b42318',
                        background: managementMessageType === 'success' ? '#e9f8f0' : '#fff0ef',
                      }}
                    >
                      {managementMessage}
                    </p>
                  )}

                  <div style={listHeaderStyle}>
                    <h3>Existing Credentials</h3>
                    <button
                      type="button"
                      className="logout-button"
                      onClick={loadCredentials}
                      disabled={credentialsLoading}
                    >
                      {credentialsLoading ? 'Refreshing...' : 'Refresh'}
                    </button>
                  </div>

                  {credentialsError && (
                    <p style={{ ...noticeStyle, color: '#b42318', background: '#fff0ef' }}>
                      {credentialsError}
                    </p>
                  )}

                  {credentialsLoading ? (
                    <p>Loading credentials...</p>
                  ) : credentials.length === 0 ? (
                    <p>No credentials found. Create your first credential above.</p>
                  ) : (
                    <div style={{ overflowX: 'auto' }}>
                      <table style={tableStyle}>
                        <thead>
                          <tr>
                            <th style={cellStyle}>Credential ID</th>
                            <th style={cellStyle}>Student</th>
                            <th style={cellStyle}>Degree</th>
                            <th style={cellStyle}>Year</th>
                            <th style={cellStyle}>Status</th>
                            <th style={cellStyle}>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {credentials.map((credential) => (
                            <tr key={credential.credentialId}>
                              <td style={cellStyle}>{credential.credentialId}</td>
                              <td style={cellStyle}>
                                {credential.studentName}
                                <br />
                                <small>{credential.studentEmail}</small>
                              </td>
                              <td style={cellStyle}>{credential.degreeName}</td>
                              <td style={cellStyle}>{credential.graduationYear}</td>
                              <td style={cellStyle}>
                                <strong>{credential.status}</strong>
                              </td>
                              <td style={cellStyle}>
                                {credential.status === 'PENDING' ? (
                                  <button
                                    type="button"
                                    className="submit-button"
                                    disabled={issuingCredentialId === credential.credentialId}
                                    onClick={() => handleIssueCredential(credential.credentialId)}
                                  >
                                    {issuingCredentialId === credential.credentialId
                                      ? 'Issuing...'
                                      : 'Issue'}
                                  </button>
                                ) : credential.status === 'ISSUED' ? (
                                  <button
                                    type="button"
                                    className="logout-button"
                                    disabled={revokingCredentialId === credential.credentialId}
                                    onClick={() => handleRevokeCredential(credential.credentialId)}
                                  >
                                    {revokingCredentialId === credential.credentialId
                                      ? 'Revoking...'
                                      : 'Revoke'}
                                  </button>
                                ) : (
                                  '—'
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
              )}
            </>
          )}

          {/* STUDENT DASHBOARD */}
          {user.role === 'STUDENT' && (
            <>
              <section className="feature-grid">
                <button
                  type="button"
                  className="feature-card"
                  onClick={() => {
                    setActiveView('my-credentials')
                    setSelectedCredential(null)
                    loadStudentCredentials()
                  }}
                >
                  <span className="feature-icon">🎓</span>
                  <h3>My Credentials</h3>
                  <p>View your academic credentials in one place.</p>
                  <strong>View credentials →</strong>
                </button>

                <article className="feature-card">
                  <span className="feature-icon">📤</span>
                  <h3>Share Credentials</h3>
                  <p>Prepare credentials to share with employers.</p>
                </article>

                <article className="feature-card">
                  <span className="feature-icon">🔐</span>
                  <h3>Secure Records</h3>
                  <p>Access your verified academic information securely.</p>
                </article>
              </section>

              {activeView === 'my-credentials' && (
                <section style={panelStyle}>
                  <div style={panelHeaderStyle}>
                    <div>
                      <p className="eyebrow">STUDENT PORTAL</p>
                      <h2>My Academic Credentials</h2>
                      <p>
                        These are the academic credentials associated with your
                        signed-in student account.
                      </p>
                    </div>

                    <button
                      type="button"
                      className="logout-button"
                      onClick={() => setActiveView('dashboard')}
                    >
                      Close
                    </button>
                  </div>

                  <div style={listHeaderStyle}>
                    <h3>Your Credentials</h3>
                    <button
                      type="button"
                      className="logout-button"
                      onClick={loadStudentCredentials}
                      disabled={studentCredentialsLoading}
                    >
                      {studentCredentialsLoading ? 'Refreshing...' : 'Refresh'}
                    </button>
                  </div>

                  {studentCredentialsLoading ? (
                    <p>Loading your credentials...</p>
                  ) : studentCredentialsError ? (
                    <p
                      role="alert"
                      style={{
                        ...noticeStyle,
                        color: '#b42318',
                        background: '#fff0ef',
                      }}
                    >
                      {studentCredentialsError}
                    </p>
                  ) : studentCredentials.length === 0 ? (
                    <div style={emptyStateStyle}>
                      <span style={{ fontSize: '36px' }}>📄</span>
                      <h3>No credentials found</h3>
                      <p>
                        No academic credentials are currently linked to your
                        account. Please contact your university administrator
                        if you believe this is incorrect.
                      </p>
                    </div>
                  ) : (
                    <div style={{ overflowX: 'auto' }}>
                      <table style={tableStyle}>
                        <thead>
                          <tr>
                            <th style={cellStyle}>Credential ID</th>
                            <th style={cellStyle}>Degree</th>
                            <th style={cellStyle}>University</th>
                            <th style={cellStyle}>Department</th>
                            <th style={cellStyle}>Graduation Year</th>
                            <th style={cellStyle}>Status</th>
                            <th style={cellStyle}>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {studentCredentials.map((credential) => (
                            <tr key={credential.credentialId}>
                              <td style={cellStyle}>
                                <strong>{credential.credentialId}</strong>
                              </td>
                              <td style={cellStyle}>{credential.degreeName}</td>
                              <td style={cellStyle}>{credential.universityName}</td>
                              <td style={cellStyle}>{credential.department}</td>
                              <td style={cellStyle}>{credential.graduationYear}</td>
                              <td style={cellStyle}>
                                <span
                                  style={{
                                    ...resultStatusStyle,
                                    color:
                                      credential.status === 'ISSUED'
                                        ? '#087443'
                                        : credential.status === 'REVOKED'
                                          ? '#b42318'
                                          : '#8a5a00',
                                    background:
                                      credential.status === 'ISSUED'
                                        ? '#e9f8f0'
                                        : credential.status === 'REVOKED'
                                          ? '#fff0ef'
                                          : '#fff7e5',
                                  }}
                                >
                                  {credential.status || 'UNKNOWN'}
                                </span>
                              </td>
                              <td style={cellStyle}>
                                <button
                                  type="button"
                                  className="logout-button"
                                  onClick={() => openCredentialDetails(credential.credentialId)}
                                >
                                  View Details
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </section>
              )}

              {/* INDIVIDUAL CREDENTIAL DETAILS */}
              {activeView === 'credential-details' && (
                <section style={panelStyle}>
                  <div style={panelHeaderStyle}>
                    <div>
                      <p className="eyebrow">STUDENT PORTAL</p>
                      <h2>Credential Details</h2>
                      <p>Detailed academic and blockchain record.</p>
                    </div>

                    <button
                      type="button"
                      className="logout-button"
                      onClick={() => {
                        setActiveView('my-credentials')
                        setSelectedCredential(null)
                        setDetailError('')
                      }}
                    >
                      Back to Credentials
                    </button>
                  </div>

                  {detailLoading ? (
                    <p>Loading credential details...</p>
                  ) : detailError ? (
                    <div
                      role="alert"
                      style={{
                        ...noticeStyle,
                        color: '#b42318',
                        background: '#fff0ef',
                      }}
                    >
                      {detailError}
                    </div>
                  ) : selectedCredential ? (
                    <>
                      <div style={resultCardStyle}>
                        <div style={resultHeaderStyle}>
                          <div>
                            <p className="eyebrow">ACADEMIC RECORD</p>
                            <h3>{selectedCredential.credentialId}</h3>
                          </div>

                          <span
                            style={{
                              ...resultStatusStyle,
                              color:
                                selectedCredential.status === 'ISSUED'
                                  ? '#087443'
                                  : selectedCredential.status === 'REVOKED'
                                    ? '#b42318'
                                    : '#8a5a00',
                              background:
                                selectedCredential.status === 'ISSUED'
                                  ? '#e9f8f0'
                                  : selectedCredential.status === 'REVOKED'
                                    ? '#fff0ef'
                                    : '#fff7e5',
                            }}
                          >
                            {selectedCredential.status || 'UNKNOWN'}
                          </span>
                        </div>

                        {selectedCredential.status === 'REVOKED' && (
                          <div
                            style={{
                              ...noticeStyle,
                              color: '#b42318',
                              background: '#fff0ef',
                            }}
                          >
                            This credential has been revoked. It must not be
                            treated as a currently valid academic credential.
                          </div>
                        )}

                        {selectedCredential.status === 'PENDING' && (
                          <div
                            style={{
                              ...noticeStyle,
                              color: '#8a5a00',
                              background: '#fff7e5',
                            }}
                          >
                            This credential is pending blockchain issuance and
                            is not yet an issued credential.
                          </div>
                        )}

                        <h3 style={{ marginTop: '24px' }}>Academic Information</h3>

                        <div style={resultGridStyle}>
                          <div style={resultItemStyle}>
                            <small>Student Name</small>
                            <strong>{selectedCredential.studentName || 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Student Email</small>
                            <strong>{selectedCredential.studentEmail || 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>University</small>
                            <strong>{selectedCredential.universityName || 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Degree</small>
                            <strong>{selectedCredential.degreeName || 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Department</small>
                            <strong>{selectedCredential.department || 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Graduation Year</small>
                            <strong>{selectedCredential.graduationYear ?? 'Not available'}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Issue Date</small>
                            <strong>{formatDate(selectedCredential.issueDate)}</strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Credential Status</small>
                            <strong>{selectedCredential.status || 'Unknown'}</strong>
                          </div>
                        </div>

                        <h3 style={{ marginTop: '28px' }}>Blockchain Verification Information</h3>

                        <div style={resultGridStyle}>
                          <div style={resultItemStyle}>
                            <small>Certificate SHA-256 Hash</small>
                            <strong style={hashTextStyle}>
                              {selectedCredential.certificateHash || 'Not available'}
                            </strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Blockchain Transaction Hash</small>
                            <strong style={hashTextStyle}>
                              {selectedCredential.blockchainTransactionHash || 'Not available'}
                            </strong>
                          </div>

                          <div style={resultItemStyle}>
                            <small>Revocation Transaction Hash</small>
                            <strong style={hashTextStyle}>
                              {selectedCredential.blockchainRevocationTransactionHash || 'Not revoked / Not available'}
                            </strong>
                          </div>
                        </div>

                        <div style={verificationMessageStyle}>
                          <strong>Important verification note</strong>
                          <p>
                            The hash and transaction reference are recorded
                            information associated with this credential.
                            Current authenticity and revocation status should
                            be checked through the CredSure verification
                            service and blockchain.
                          </p>
                        </div>
                      </div>

                      <div style={certificateActionStyle}>
                        <button
                          type="button"
                          className="submit-button"
                          style={{ maxWidth: '280px' }}
                          onClick={handleDownloadCertificate}
                          disabled={selectedCredential.status !== 'ISSUED'}
                        >
                          {selectedCredential.status === 'ISSUED'
                            ? 'Download Certificate (PDF)'
                            : 'Certificate Unavailable'}
                        </button>

                        <p style={{ color: '#8490a3', fontSize: '12px', lineHeight: 1.6 }}>
                          The button opens a print window. Choose "Save as PDF"
                          in your browser's print dialog.
                        </p>
                      </div>
                    </>
                  ) : (
                    <p>No credential details are available.</p>
                  )}
                </section>
              )}

              <p className="dashboard-note">
                Your credentials are retrieved securely using your authenticated
                student account.
              </p>
            </>
          )}

          {/* EMPLOYER DASHBOARD */}
          {user.role === 'EMPLOYER' && (
            <>
              <section className="feature-grid">
                <button
                  type="button"
                  className="feature-card"
                  onClick={openVerification}
                >
                  <span className="feature-icon">🔎</span>
                  <h3>Verify Credentials</h3>
                  <p>Check whether an academic credential is valid.</p>
                  <strong>Verify now →</strong>
                </button>

                <button
                  type="button"
                  className="feature-card"
                  onClick={openVerification}
                >
                  <span className="feature-icon">⛓️</span>
                  <h3>Blockchain Validation</h3>
                  <p>Check credential records against the blockchain.</p>
                  <strong>Check blockchain →</strong>
                </button>

                <button
                  type="button"
                  className="feature-card"
                  onClick={openVerification}
                >
                  <span className="feature-icon">🛡️</span>
                  <h3>Trusted Results</h3>
                  <p>Identify issued, revoked, and invalid credentials.</p>
                  <strong>View results →</strong>
                </button>
              </section>

              {activeView === 'verify' && (
                <section style={panelStyle}>
                  <div style={panelHeaderStyle}>
                    <div>
                      <p className="eyebrow">EMPLOYER VERIFICATION</p>
                      <h2>Verify Academic Credentials</h2>
                      <p>
                        Enter a credential ID to check its database and blockchain
                        verification status.
                      </p>
                    </div>

                    <button
                      type="button"
                      className="logout-button"
                      onClick={() => setActiveView('dashboard')}
                    >
                      Close
                    </button>
                  </div>

                  <form onSubmit={handleVerifyCredential} style={verificationFormStyle}>
                    <label style={fieldStyle}>
                      Credential ID
                      <input
                        type="text"
                        value={verificationId}
                        onChange={(event) => {
                          setVerificationId(event.target.value)
                          setVerificationResult(null)
                          setVerificationError('')
                        }}
                        placeholder="e.g. CRED-2026-040"
                        required
                      />
                    </label>

                    <button
                      type="submit"
                      className="submit-button"
                      disabled={verificationLoading}
                    >
                      {verificationLoading ? 'Verifying...' : 'Verify Credential'}
                    </button>
                  </form>

                  {verificationError && (
                    <div
                      role="alert"
                      style={{
                        ...noticeStyle,
                        color: '#b42318',
                        background: '#fff0ef',
                      }}
                    >
                      {verificationError}
                    </div>
                  )}

                  {verificationResult && (
                    <div style={resultCardStyle}>
                      <div style={resultHeaderStyle}>
                        <div>
                          <p className="eyebrow">VERIFICATION RESULT</p>
                          <h3>
                            {verificationResult.valid
                              ? 'Credential Verified'
                              : 'Credential Not Valid'}
                          </h3>
                        </div>

                        <span
                          style={{
                            ...resultStatusStyle,
                            color: verificationResult.valid ? '#087443' : '#b42318',
                            background: verificationResult.valid ? '#e9f8f0' : '#fff0ef',
                          }}
                        >
                          {verificationResult.status || 'UNKNOWN'}
                        </span>
                      </div>

                      <div style={resultGridStyle}>
                        <div style={resultItemStyle}>
                          <small>Credential ID</small>
                          <strong>{verificationResult.credentialId || verificationId}</strong>
                        </div>

                        <div style={resultItemStyle}>
                          <small>Student Name</small>
                          <strong>{verificationResult.studentName || 'Not available'}</strong>
                        </div>

                        <div style={resultItemStyle}>
                          <small>University</small>
                          <strong>{verificationResult.universityName || 'Not available'}</strong>
                        </div>

                        <div style={resultItemStyle}>
                          <small>Degree</small>
                          <strong>{verificationResult.degreeName || 'Not available'}</strong>
                        </div>

                        <div style={resultItemStyle}>
                          <small>Graduation Year</small>
                          <strong>{verificationResult.graduationYear ?? 'Not available'}</strong>
                        </div>

                        <div style={resultItemStyle}>
                          <small>Verification Status</small>
                          <strong>{verificationResult.status || 'Unknown'}</strong>
                        </div>
                      </div>

                      <div style={verificationMessageStyle}>
                        <strong>Verification Message</strong>
                        <p>
                          {verificationResult.message || 'No additional verification details were returned.'}
                        </p>
                      </div>
                    </div>
                  )}
                </section>
              )}
            </>
          )}
        </main>
      </div>
    )
  }

  // LOGIN AND REGISTRATION PAGE
  return (
    <div className="app-shell">
      <header className="topbar">
        <a className="brand" href="#home">
          <span className="brand-icon">◆</span>
          <span>Cred<span className="brand-accent">Sure</span></span>
        </a>

        <span className="topbar-caption">Academic Credential Verification</span>
      </header>

      <main className="auth-layout">
        <section className="intro-panel">
          <div className="intro-content">
            <p className="eyebrow">TRUST. TRANSPARENCY. TECHNOLOGY.</p>
            <h1>
              Your academic
              <br />
              achievements,
              <br />
              <span>verified with trust.</span>
            </h1>
            <p className="intro-description">
              A secure academic credential verification platform powered by
              blockchain technology. Issue, access, and verify educational
              credentials with confidence.
            </p>

            <div className="trust-points">
              <div className="trust-point">
                <span>✓</span>
                Blockchain-backed verification
              </div>
              <div className="trust-point">
                <span>✓</span>
                Secure role-based access
              </div>
              <div className="trust-point">
                <span>✓</span>
                Reliable credential records
              </div>
            </div>
          </div>

          <div className="intro-footer">
            <span className="footer-shield">◇</span>
            Built for a more trustworthy academic ecosystem
          </div>
        </section>

        <section className="auth-panel">
          <div className="auth-card">
            <div className="auth-heading">
              <div className="auth-symbol">◆</div>
              <p className="eyebrow">WELCOME TO CREDSURE</p>
              <h2>{isRegistering ? 'Create your account' : 'Sign in to continue'}</h2>
              <p>
                {isRegistering
                  ? 'Register as a student or employer to get started.'
                  : 'Choose your account type and access your workspace.'}
              </p>
            </div>

            <div className="role-selector">
              {roles.map((role) => (
                <button
                  key={role.value}
                  type="button"
                  className={`role-option ${selectedRole === role.value ? 'selected' : ''}`}
                  onClick={() => {
                    setSelectedRole(role.value)
                    setMessage('')
                  }}
                >
                  <span className="role-icon">{role.icon}</span>
                  <span className="role-text">
                    <strong>{role.title}</strong>
                    <small>{role.description}</small>
                  </span>
                  <span className="role-radio">
                    {selectedRole === role.value ? '●' : '○'}
                  </span>
                </button>
              ))}
            </div>

            <form className="auth-form" onSubmit={handleSubmit}>
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                autoComplete="email"
                required
              />

              <div className="password-label-row">
                <label htmlFor="password">Password</label>
              </div>
              <input
                id="password"
                type="password"
                placeholder="Enter your password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete={isRegistering ? 'new-password' : 'current-password'}
                required
              />

              {isRegistering && (
                <>
                  <label htmlFor="confirmPassword">Confirm password</label>
                  <input
                    id="confirmPassword"
                    type="password"
                    placeholder="Enter your password again"
                    value={confirmPassword}
                    onChange={(event) => setConfirmPassword(event.target.value)}
                    autoComplete="new-password"
                    required
                  />
                </>
              )}

              {message && (
                <div className={`form-message ${messageType}`} role="alert">
                  {message}
                </div>
              )}

              <button className="submit-button" type="submit" disabled={loading}>
                {loading
                  ? 'Please wait...'
                  : isRegistering
                    ? 'Create account'
                    : 'Sign in'}
                {!loading && <span>→</span>}
              </button>
            </form>

            {selectedRole !== 'UNIVERSITY_ADMIN' && (
              <p className="switch-auth">
                {isRegistering ? 'Already have an account?' : "Don't have an account?"}{' '}
                <button
                  type="button"
                  onClick={() => {
                    setIsRegistering(!isRegistering)
                    setPassword('')
                    setConfirmPassword('')
                    setMessage('')
                  }}
                >
                  {isRegistering ? 'Sign in' : 'Register'}
                </button>
              </p>
            )}

            <div className="auth-security">
              <span>🔐</span>
              Your session is protected with secure authentication.
            </div>
          </div>
        </section>
      </main>

      <footer className="page-footer">
        <span>© 2026 CredSure</span>
        <span>Academic Credential Verification System</span>
      </footer>
    </div>
  )
}

// Shared styles
const panelStyle = {
  marginTop: '28px',
  padding: '28px',
  background: '#ffffff',
  border: '1px solid #e3e9f2',
  borderRadius: '16px',
  boxShadow: '0 8px 28px rgba(30, 50, 80, 0.06)',
}

const panelHeaderStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'flex-start',
  gap: '16px',
  marginBottom: '28px',
  flexWrap: 'wrap',
}

const formGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
  gap: '18px',
  marginTop: '20px',
  marginBottom: '24px',
}

const fieldStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '8px',
  fontWeight: 600,
  fontSize: '14px',
}

const noticeStyle = {
  padding: '12px 16px',
  borderRadius: '8px',
  margin: '16px 0',
}

const listHeaderStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: '12px',
  marginTop: '32px',
  marginBottom: '16px',
}

const tableStyle = {
  width: '100%',
  borderCollapse: 'collapse',
  textAlign: 'left',
  fontSize: '14px',
}

const cellStyle = {
  padding: '13px 12px',
  borderBottom: '1px solid #e8edf4',
  verticalAlign: 'top',
}

const verificationFormStyle = {
  display: 'grid',
  gridTemplateColumns: 'minmax(220px, 1fr) auto',
  alignItems: 'end',
  gap: '16px',
  marginBottom: '24px',
}

const resultCardStyle = {
  marginTop: '24px',
  padding: '24px',
  border: '1px solid #e3e9f2',
  borderRadius: '12px',
  background: '#fbfcfe',
}

const resultHeaderStyle = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: '16px',
  flexWrap: 'wrap',
  marginBottom: '24px',
}

const resultStatusStyle = {
  padding: '8px 14px',
  borderRadius: '20px',
  fontWeight: 700,
  fontSize: '13px',
}

const resultGridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
  gap: '18px',
}

const resultItemStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '7px',
  padding: '14px',
  borderRadius: '8px',
  background: '#ffffff',
  border: '1px solid #e8edf4',
  overflowWrap: 'anywhere',
}

const verificationMessageStyle = {
  marginTop: '20px',
  padding: '16px',
  borderRadius: '8px',
  background: '#eef4ff',
  color: '#243d69',
  lineHeight: 1.7,
}

const emptyStateStyle = {
  marginTop: '20px',
  padding: '32px 20px',
  textAlign: 'center',
  background: '#f8faff',
  border: '1px dashed #d5deeb',
  borderRadius: '12px',
}

const hashTextStyle = {
  fontSize: '12px',
  lineHeight: 1.7,
  overflowWrap: 'anywhere',
  wordBreak: 'break-word',
}

const certificateActionStyle = {
  display: 'flex',
  alignItems: 'center',
  flexWrap: 'wrap',
  gap: '18px',
  marginTop: '22px',
}

// Blockchain status card styles
const blockchainCardContentStyle = {
  marginTop: '18px',
  display: 'flex',
  flexDirection: 'column',
  gap: '13px',
  textAlign: 'left',
}

const blockchainStatusRowStyle = {
  display: 'flex',
  alignItems: 'center',
  gap: '9px',
  fontSize: '14px',
}

const blockchainIndicatorStyle = {
  width: '10px',
  height: '10px',
  borderRadius: '50%',
  display: 'inline-block',
  flexShrink: 0,
}

const blockchainInfoStyle = {
  display: 'flex',
  flexDirection: 'column',
  gap: '5px',
  padding: '10px',
  borderRadius: '8px',
  background: '#f7f9fc',
  border: '1px solid #e8edf4',
  fontSize: '12px',
}

const blockchainValueStyle = {
  fontSize: '11px',
  lineHeight: 1.6,
  overflowWrap: 'anywhere',
  wordBreak: 'break-word',
}

const blockchainMessageStyle = {
  margin: 0,
  color: '#087443',
  fontSize: '12px',
  lineHeight: 1.6,
}

const blockchainErrorStyle = {
  margin: 0,
  padding: '10px',
  borderRadius: '8px',
  background: '#fff0ef',
  color: '#b42318',
  fontSize: '12px',
  lineHeight: 1.6,
  overflowWrap: 'anywhere',
}

const blockchainLastCheckedStyle = {
  margin: 0,
  color: '#8490a3',
  fontSize: '11px',
}

const blockchainRefreshButtonStyle = {
  width: '100%',
  marginTop: '4px',
}

export default App
