import axios from 'axios';

const connectToEndpoint = async (domain: string, endpoint: string): Promise<number> => {
  const response = await axios.get(`${domain}/${endpoint}`)
  return response.status
}

export default connectToEndpoint