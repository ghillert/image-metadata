const path = require('node:path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
module.exports = {
  entry: './src/main/frontend/app.js',
  output: { path: path.resolve(__dirname, 'target/frontend/assets'), filename: 'app.js', clean: true },
  plugins: [new MiniCssExtractPlugin({ filename: 'app.css' })],
  module: { rules: [
    { test: /\.css$/i, use: [MiniCssExtractPlugin.loader, 'css-loader'] },
    { test: /\.(png|svg|woff2?|ttf)$/i, type: 'asset/resource' }
  ] },
  performance: { hints: false }
};
